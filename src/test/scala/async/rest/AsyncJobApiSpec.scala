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

class AsyncJobApiSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  "AsyncJobApi" should {
    "handle HEAD request which returns count of job items" in {
      val from = "2026-01-21T12:11:00Z"
      val to   = "2026-01-28T17:05:00Z"

      val jobProcessor = mock[JobProcessor]
      when {
        jobProcessor.count(any[Instant], any[Instant])
      }.thenReturn(IO.pure(42))
      val api = AsyncJobApi(jobProcessor)

      val request = Request[IO](
        Method.HEAD,
        uri"/jobs".withQueryParam("from", from).withQueryParam("to", to)
      )
      val response = api.routes.orNotFound.run(request)
      response.asserting { resp =>
        resp.status shouldBe Status.Accepted
        verify(jobProcessor).count(is(Instant.parse(from)), is(Instant.parse(to)))
        resp.headers.get(ci"X-Total-Count").map(_.head.value) shouldBe Some("42")
      }
    }
  }
}
