package async.rest

import cats.effect.IO
import org.http4s.{Header, Response}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.shouldBe

def checkHeader[H: [h] =>> Header[h, Header.Single], V](
    response: Response[IO],
    expected: Option[V]
  )(f: H => V): IO[Assertion] = 
  IO(response.headers.get[H].map(f) shouldBe expected)
