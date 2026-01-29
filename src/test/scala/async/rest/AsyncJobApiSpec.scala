package async.rest

import async.service.JobProcessor
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as is}
import org.typelevel.ci.*
import java.time.Instant
import org.http4s.circe.*
import io.circe.literal.*
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType

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

  "AsyncJobApi" should :
    "handle HEAD request which returns count of job items" in :
      val jobProcessor = mock[JobProcessor]
      when :
        jobProcessor.count(any[Instant], any[Instant])
      .thenReturn(IO.pure(42))

      val api = AsyncJobApi(jobProcessor)

      val request = Request[IO](Method.HEAD, uri"/jobs")
        .withEntity(jobRequest)
        .withHeaders(`Content-Type`(MediaType.application.json))

      api.routes.orNotFound.run(request).asserting : resp =>
        resp.status shouldBe Status.Accepted
        verify(jobProcessor).count(is(from), is(to))
        resp.headers.get(ci"X-Total-Count").map(_.head.value) shouldBe Some("42")


}
