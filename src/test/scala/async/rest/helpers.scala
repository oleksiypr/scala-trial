package async.rest

import cats.effect.IO
import org.http4s.{Header, Response}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.shouldBe
import cats.syntax.all.*

def checkHeader[H: [h] =>> Header[h, Header.Single], V](
    response: Response[IO],
    expected: V
  )(f: H => V): IO[Assertion] = 
  IO(response.headers.get[H].map(f) shouldBe expected.some)
  
def checkNoHeader[H: [h] =>> Header[h, Header.Single]](
    response: Response[IO]
  ): IO[Assertion] = IO(response.headers.get[H] shouldBe None)
