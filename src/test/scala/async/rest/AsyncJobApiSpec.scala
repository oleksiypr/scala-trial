package async.rest

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.typelevel.ci.*

class AsyncJobApiSpec extends AsyncWordSpec
  with AsyncIOSpec with Matchers with MockitoSugar {

  "AsyncJobApi" should {
    "handle HEAD request which returns count of job items" in {
      val request = Request[IO](Method.HEAD, uri"/jobs")
      val response = AsyncJobApi.routes.orNotFound.run(request)
      response.asserting { resp =>
        resp.status shouldBe Status.Accepted
        resp.headers.get(ci"X-Total-Count").map(_.head.value) shouldBe Some("42")
      }
    }
  }
}
