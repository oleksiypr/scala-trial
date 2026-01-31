package async.rest

import async.common.TimeRange
import async.service.JobService
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Deferred, IO}
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
import scala.concurrent.duration.DurationInt

class AsyncJobApiSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  val jobId = UUID.fromString("48bf7b76-00aa-4583-b8d6-d63c1830696f")
  val from  = Instant.parse("2026-01-21T12:11:00Z")
  val to    = Instant.parse("2026-01-28T17:05:00Z")
  val query = TimeRange(from, to)
  val count = 42L
  val job   = JobService.Job(jobId, count, query: TimeRange)

  val request = Request[IO](Method.POST, uri"/jobs")
    .withEntity(
      json"""
      {
        "from": "2026-01-21T12:11:00Z",
        "to":   "2026-01-28T17:05:00Z"
      }
    """)
    .withHeaders(`Content-Type`(MediaType.application.json))

  "POST /job" should {
    "responds with HTTP headers and do the job synchronously" in {

      def checkResponse(response: Response[IO], jobService: JobService) = IO {
        response.status shouldBe Status.Accepted
        response.headers.get[Location].map(_.uri) shouldBe (uri"/jobs" / jobId).some
        response.headers.get[`X-Total-Count`].map(_.count) shouldBe count.some
      }

      val test = for
        jobService <- setup()
        api         = AsyncJobApi(jobService)
        response   <- api.routes.orNotFound.run(request)
        assertion  <- checkResponse(response, jobService)
        _          <- IO(verify(jobService).prepare(is(query)))
        _          <- IO(verify(jobService).process(is(job)))
      yield
        assertion

      test
    }
  }

  private def setup() = IO {
    val jobService = mock[JobService]
    when:
      jobService.prepare(any[TimeRange])
    .thenReturn:
      IO.pure(job)

    when:
      jobService.process(any[JobService.Job])
    .thenReturn:
      IO.pure(40L)

    jobService
  }
}
