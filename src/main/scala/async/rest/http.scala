package async.rest

import cats.effect.IO
import org.http4s.{Header, Headers, ParseResult, Request, Response}
import org.typelevel.ci.CIString
import org.typelevel.ci.*

final case class `X-Total-Count`(count: Long)

object `X-Total-Count` {
  given Header[`X-Total-Count`, Header.Single] =
    Header.create(
      ci"X-Total-Count",
      _.count.toString,
      s => ParseResult.fromTryCatchNonFatal("Invalid X-Total-Count")(
        `X-Total-Count`(s.toLong)
      )
    )
}

extension (response: IO[Response[IO]])
  def putHeader[T: [t] =>> Header[t, ?]](header: T): IO[Response[IO]] =
    response.map(_.putHeaders(header))

  def putHeaders(headers: (CIString, String)*): IO[Response[IO]] =
    response.map :
      _.putHeaders :
        headers.map : hs =>
          Header.Raw(hs._1, hs._2)

