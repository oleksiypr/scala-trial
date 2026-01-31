package async.rest

import async.common.{Logger, TimeRange}
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
  
  import JobService.*

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
    "initiates the job in parallel and responds with HTTP headers immediately" in {

      def checkResponse(response: Response[IO], jobService: JobService) = IO {
        response.status shouldBe Status.Accepted
        response.headers.get[Location].map(_.uri) shouldBe (uri"/jobs" / jobId).some
        response.headers.get[`X-Total-Count`].map(_.count) shouldBe count.some
      }

      val test = for
        jobResult  <- Deferred[IO, JobResult]
        deps       <- setup(jobResult)
        api         = AsyncJobApi(deps.jobService, deps.logger)
        response   <- api.routes.orNotFound.run(request).timeout(200.millis)
        assertion  <- checkResponse(response, deps.jobService)
        _          <- verifyIO(deps.jobService)(_.prepare(is(query)))
        _          <- verifyIO(deps.jobService)(_.process(is(job)))
      yield
        assertion

      test
    }

    "log job result asynchronously when job completes" in {
      for
        jobResult <- Deferred[IO, JobResult]
        deps      <- setup(jobResult)
        api        = AsyncJobApi(deps.jobService, deps.logger)
        response  <- api.routes.orNotFound.run(request).timeout(100.millis)
        assertion <- IO(response.status shouldBe Status.Accepted)
        _         <- jobResult.complete(JobResult(jobId, processed = 40L))
        _         <- verifyIO(deps.logger):
                      _.info(is(s"[Async] [POST] [/jobs] id: $jobId, items processed: 40"))
      yield
        assertion
    }
  }

  private def verifyIO[R, A](r: R)(f: R => A): IO[A] =
    IO(verify(r, timeout(100).times(1))).map(f)

  private def setup(jobResult: Deferred[IO, JobService.JobResult]) = IO {
    val jobService = mock[JobService]
    val logger     = mock[Logger]
    when:
      jobService.prepare(any[TimeRange])
    .thenReturn:
      IO.pure(job)

    when:
      jobService.process(any[JobService.Job])
    .thenReturn:
      jobResult.get

    when:
      logger.info(any[String])
    .thenReturn:
      IO.unit

    (jobService = jobService, logger = logger)
  }
}
