package async.rest

import async.service.CancellableService
import async.service.CancellableService.{JobSnapshot, JobStarted, JobStatus}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import org.http4s.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.mockito.ArgumentMatchers.eq as is
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import java.util.UUID

class CancellableJobApiSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  private val jobId   = UUID.fromString("48bf7b76-00aa-4583-b8d6-d63c1830696f")
  private val missing = UUID.fromString("dbf6f219-0b85-4fcf-82a3-f8bd4f001f6f")

  "POST /jobs" should {
    "return 202 with Location, X-Job-Id and X-Total-Count headers" in {
      val started = JobStarted(jobId, totalCount = 42L)
      val request = Request[IO](Method.POST, uri"/jobs")
      val setup = IO {
        val service = mock[CancellableService]
        when(service.start()).thenReturn(IO.pure(started))
        service
      }

      for
        service  <- setup
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
        _        <- IO(response.status shouldBe Status.Accepted)
        _        <- checkHeader[Location, Uri](response, uri"/jobs" / jobId)(_.uri)
        _        <- checkHeader[`X-Job-Id`, UUID](response, jobId)(_.jobId)
        _        <- checkHeader[`X-Total-Count`, Long](response, 42L)(_.count)
      yield
        ()
    }
  }

  "DELETE /jobs/{jobId}" should {
    "return 204 with cancellation headers when cancellation succeeds" in {
      val cancelled = JobSnapshot(jobId, doneCount = 12L, status = JobStatus.Cancelled)
      val request   = Request[IO](Method.DELETE, uri"/jobs" / jobId.toString)
      val setup = IO {
        val service = mock[CancellableService]
        when(service.cancel(is(jobId))).thenReturn(IO.pure(cancelled.some))
        service
      }

      for
        service  <- setup
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
        _        <- IO(response.status shouldBe Status.NoContent)
        _        <- checkHeader[`X-Job-Id`, UUID](response, jobId)(_.jobId)
        _        <- checkHeader[`X-Done-Count`, Long](response, 12L)(_.count)
        _        <- checkHeader[`X-Job-Status`, String](response, "cancelled")(_.status)
      yield
        ()
    }

    "remain idempotent for terminal states and expose current terminal status" in {
      val request = Request[IO](Method.DELETE, uri"/jobs" / jobId.toString)

      val terminalStates = List(
        JobStatus.Cancelled -> "cancelled",
        JobStatus.Failed    -> "failed",
        JobStatus.Completed -> "completed"
      )

      def setup(state: JobStatus) = IO {
        val service = mock[CancellableService]
        when:
          service.cancel(is(jobId))
        .thenReturn:
          IO.pure(JobSnapshot(jobId, doneCount = 9L, status = state).some)

        service
      }

      terminalStates.traverse_ { (state, expectedStatus) =>
        for
          service  <- setup(state)
          api       = CancellableJobApi(service)
          response <- api.routes.orNotFound.run(request)
          _        <- IO(response.status shouldBe Status.NoContent)
          _        <- checkHeader[`X-Job-Status`, String](response, expectedStatus)(_.status)
        yield ()
      }
    }

    "return 404 when job is missing" in {
      val request = Request[IO](Method.DELETE, uri"/jobs" / missing.toString)
      val setup = IO {
        val service = mock[CancellableService]
        when(service.cancel(is(missing))).thenReturn(IO.pure(None))
        service
      }

      for
        service  <- setup
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
      yield
        response.status shouldBe Status.NotFound
    }
  }

  "HEAD /jobs/{jobId}" should {
    "return 200 for running, cancelled, failed and completed states with expected headers" in {
      val request = Request[IO](Method.HEAD, uri"/jobs" / jobId.toString)

      val cases = List(
        JobSnapshot(jobId, doneCount = 3L, JobStatus.Running,   failureReason = None)                -> "running",
        JobSnapshot(jobId, doneCount = 4L, JobStatus.Cancelled, failureReason = None)                -> "cancelled",
        JobSnapshot(jobId, doneCount = 5L, JobStatus.Failed,    failureReason = Some("Failure_001")) -> "failed",
        JobSnapshot(jobId, doneCount = 6L, JobStatus.Completed, failureReason = None)                -> "completed"
      )

      def setup(snapshot: JobSnapshot) = IO {
        val service = mock[CancellableService]
        when(service.status(is(jobId))).thenReturn(IO.pure(snapshot.some))
        service
      }

      cases.traverse_ { (snapshot, expectedStatus) =>
        for
          service  <- setup(snapshot)
          api       = CancellableJobApi(service)
          response <- api.routes.orNotFound.run(request)
          _        <- IO(response.status shouldBe Status.Ok)
          _        <- checkHeader[`X-Job-Id`, UUID](response, jobId)(_.jobId)
          _        <- checkHeader[`X-Done-Count`, Long](response, snapshot.doneCount)(_.count)
          _        <- checkHeader[`X-Job-Status`, String](response, expectedStatus)(_.status)
          _        <- {
            if expectedStatus == "failed" then
              checkHeader[`X-Failure-Reason`, String](response, "Failure_001")(_.reason)
            else
              checkNoHeader[`X-Failure-Reason`](response)
          }
        yield ()
      }
    }

    "return 404 when job is missing" in {
      val request = Request[IO](Method.HEAD, uri"/jobs" / missing.toString)
      val setup = IO {
        val service = mock[CancellableService]
        when(service.status(is(missing))).thenReturn(IO.pure(None))
        service
      }

      for
        service  <- setup
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
      yield
        response.status shouldBe Status.NotFound
    }
  }
}
