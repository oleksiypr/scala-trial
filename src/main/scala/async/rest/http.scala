package async.rest

import cats.effect.IO
import org.http4s.{Header, Request, Response}
import org.typelevel.ci.CIString

private type HeaderNameValue = (CIString, String)

extension (response: IO[Response[IO]])
  def putHeader(name: CIString, value: String): IO[Response[IO]] =
    putHeaders((name, value))

  def putHeaders(headers: HeaderNameValue*): IO[Response[IO]] =
    response.map :
      _.putHeaders :
        headers.map : hs =>
          Header.Raw(hs._1, hs._2)

extension (request: Request[IO])
  def getHeader[T](headerName: CIString)(f: String => T): Option[T] =
    request.headers.get(headerName).map(_.head.value).map(f)

