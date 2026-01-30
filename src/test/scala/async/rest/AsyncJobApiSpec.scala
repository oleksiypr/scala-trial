package async.rest

import async.service.JobService
import cats.effect.{Deferred, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.implicits.*
import org.mockito.ArgumentMatchers.{any, eq as is}
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.DurationInt

class AsyncJobApiSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  val from = Instant.parse("2026-01-21T12:11:00Z")
  val to   = Instant.parse("2026-01-28T17:05:00Z")

  val jobRequest =
    json"""
    {
      "from": "2026-01-21T12:11:00Z",
      "to":   "2026-01-28T17:05:00Z"
    }
  """

  "POST /job" should {
    "initiate the job in parallel, respond with count of job items in headers" in {
      val jobId = UUID.fromString("48bf7b76-00aa-4583-b8d6-d63c1830696f")
      val count = 42

      val request = Request[IO](Method.POST, uri"/jobs")
        .withEntity(jobRequest)
        .withHeaders(`Content-Type`(MediaType.application.json))

      def deferredSetup(jobResult: Deferred[IO, Unit]): IO[JobService] = IO {
        val jobService = mock[JobService]
        when:
          jobService.prepare(any[Instant], any[Instant])
        .thenReturn:
          IO.pure(JobService.Job(count, jobId))

        when:
          jobService.process(any[Instant], any[Instant])
        .thenReturn:
          jobResult.get

        jobService
      }

      val test = for
        jobResult    <- Deferred[IO, Unit]
        jobProcessor <- deferredSetup(jobResult)
        api           = AsyncJobApi(jobProcessor)
        response     <- api.routes.orNotFound.run(request)
      yield
        (response, jobProcessor)

      test.timeoutTo(200.millis, IO.raiseError(new TimeoutException))
        .asserting: (resp, jobService) =>
          resp.status shouldBe Status.Accepted
          verify(jobService).prepare(is(from), is(to))
          verify(jobService).process(is(from), is(to))
          resp.headers.get[Location].map(_.uri) shouldBe (uri"/jobs" / jobId).some
          resp.headers.get[`X-Total-Count`].map(_.count) shouldBe count.some
    }
  }
}
