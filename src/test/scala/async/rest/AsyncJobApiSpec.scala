package async.rest

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import cats.effect.IO
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import cats.effect.unsafe.implicits.global
import org.typelevel.ci.CIString

class AsyncJobApiSpec extends AnyWordSpecLike with Matchers {
  "AsyncJobApi" should {
    "handle HEAD request which returns count of job items" in {
      val request = Request[IO](Method.HEAD, uri"/jobs")
      val response = AsyncJobApi.routes.orNotFound.run(request).unsafeRunSync()
      response.status shouldEqual Status.Accepted
    }
  }
}


