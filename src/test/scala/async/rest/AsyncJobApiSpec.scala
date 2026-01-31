package async.rest

import async.common.{Logger, TimeRange}
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
    "initiate the job in parallel, respond with count of job items in headers" in {

      val test = for
        jobResult <- Deferred[IO, Unit]
        deps      <- deferredSetup(jobResult)
        api        = AsyncJobApi(deps.jobService, deps.logger)
        response  <- api.routes.orNotFound.run(request)
      yield
        (response, deps.jobService)

      test.timeoutTo(200.millis, IO.raiseError(new TimeoutException))
        .asserting: (resp, jobService) =>
          resp.status shouldBe Status.Accepted
          verify(jobService).prepare(is(query))
          verify(jobService).process(is(job))
          resp.headers.get[Location].map(_.uri) shouldBe (uri"/jobs" / jobId).some
          resp.headers.get[`X-Total-Count`].map(_.count) shouldBe count.some
    }

    "notify asynchronously when the job is done" in {
      val test = for
        jobResult <- Deferred[IO, Unit]
        deps      <- deferredSetup(jobResult)
        api        = AsyncJobApi(deps.jobService, deps.logger)
        response  <- api.routes.orNotFound.run(request)
        _         <- jobResult.complete(())
      yield
        (response, deps.logger)

      test.timeoutTo(200.millis, IO.raiseError(new TimeoutException))
        .asserting: (resp, logger) =>
          verify(logger).info(is(s"[REQ] [POST] path = /jobs: Job $jobId, 40 items completed."))
          resp.status shouldBe Status.Accepted
    }
  }

  private def deferredSetup(jobResult: Deferred[IO, Unit]) = IO {
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
