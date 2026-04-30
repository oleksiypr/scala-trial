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
      yield
        response.status shouldBe Status.Accepted
        response.headers.get[Location].map(_.uri) shouldBe (uri"/jobs" / jobId).some
        response.headers.get[`X-Job-Id`].map(_.jobId) shouldBe jobId.some
        response.headers.get[`X-Total-Count`].map(_.count) shouldBe 42L.some
    }
  }

  "DELETE /jobs/{jobId}" should {
    "return 204 with cancellation headers when cancellation succeeds" in {
      val cancelled = JobSnapshot(jobId, doneCount = 12L, status = JobStatus.Cancelled)
      val request   = Request[IO](Method.DELETE, uri"/jobs" / jobId.toString)

      for
        service  <- IO(mock[CancellableService])
        _        <- IO(when(service.cancel(is(jobId))).thenReturn(IO.pure(cancelled.some)))
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
      yield
        response.status shouldBe Status.NoContent
        response.headers.get[`X-Job-Id`].map(_.jobId) shouldBe jobId.some
        response.headers.get[`X-Done-Count`].map(_.count) shouldBe 12L.some
        response.headers.get[`X-Job-Status`].map(_.status) shouldBe "cancelled".some
    }

    "remain idempotent for terminal states and expose current terminal status" in {
      val terminalStates = List(
        JobStatus.Cancelled -> "cancelled",
        JobStatus.Failed    -> "failed",
        JobStatus.Completed -> "completed"
      )

      val request = Request[IO](Method.DELETE, uri"/jobs" / jobId.toString)

      terminalStates.traverse_ { case (state, expectedStatus) =>
        for
          service  <- IO(mock[CancellableService])
          _        <- IO(when(service.cancel(is(jobId))).thenReturn(IO.pure(JobSnapshot(jobId, 9L, state).some)))
          api       = CancellableJobApi(service)
          response <- api.routes.orNotFound.run(request)
          _        <- IO(response.status shouldBe Status.NoContent)
          _        <- IO(response.headers.get[`X-Job-Status`].map(_.status) shouldBe expectedStatus.some)
        yield ()
      }
    }

    "return 404 when job is missing" in {
      val request = Request[IO](Method.DELETE, uri"/jobs" / missing.toString)

      for
        service  <- IO(mock[CancellableService])
        _        <- IO(when(service.cancel(is(missing))).thenReturn(IO.pure(None)))
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
      yield
        response.status shouldBe Status.NotFound
    }
  }

  "HEAD /jobs/{jobId}" should {
    "return 200 for running, cancelled, failed and completed states with expected headers" in {
      val cases = List(
        JobSnapshot(jobId, 3L, JobStatus.Running, None)         -> "running",
        JobSnapshot(jobId, 4L, JobStatus.Cancelled, None)       -> "cancelled",
        JobSnapshot(jobId, 5L, JobStatus.Failed, Some("boom")) -> "failed",
        JobSnapshot(jobId, 6L, JobStatus.Completed, None)       -> "completed"
      )

      val request = Request[IO](Method.HEAD, uri"/jobs" / jobId.toString)

      cases.traverse_ { case (snapshot, expectedStatus) =>
        for
          service  <- IO(mock[CancellableService])
          _        <- IO(when(service.status(is(jobId))).thenReturn(IO.pure(snapshot.some)))
          api       = CancellableJobApi(service)
          response <- api.routes.orNotFound.run(request)
          _        <- IO(response.status shouldBe Status.Ok)
          _        <- IO(response.headers.get[`X-Job-Id`].map(_.jobId) shouldBe jobId.some)
          _        <- IO(response.headers.get[`X-Done-Count`].map(_.count) shouldBe snapshot.doneCount.some)
          _        <- IO(response.headers.get[`X-Job-Status`].map(_.status) shouldBe expectedStatus.some)
          _        <- IO(
                        if expectedStatus == "failed" then
                          response.headers.get[`X-Failure-Reason`].map(_.reason) shouldBe "boom".some
                        else
                          response.headers.get[`X-Failure-Reason`] shouldBe None
                      )

        yield ()
      }
    }

    "return 404 when job is missing" in {
      val request = Request[IO](Method.HEAD, uri"/jobs" / missing.toString)

      for
        service  <- IO(mock[CancellableService])
        _        <- IO(when(service.status(is(missing))).thenReturn(IO.pure(None)))
        api       = CancellableJobApi(service)
        response <- api.routes.orNotFound.run(request)
      yield
        response.status shouldBe Status.NotFound
    }
  }

}

