package async.rest

import cats.effect.IO
import org.http4s.{Header, Headers, Request, Response}
import org.typelevel.ci.CIString

private type HeaderNameValue = (CIString, String)

private def getValue(
    headerName: CIString, 
    headers: Headers
  ): Option[String] = headers.get(headerName).map(_.head.value)

extension (response: IO[Response[IO]])
  def putHeader(name: CIString, value: String): IO[Response[IO]] =
    putHeaders((name, value))

  def putHeaders(headers: HeaderNameValue*): IO[Response[IO]] =
    response.map :
      _.putHeaders :
        headers.map : hs =>
          Header.Raw(hs._1, hs._2)

extension (request: Request[IO])
  def getHeaderValue(headerName: CIString): Option[String] =
    getValue(headerName, request.headers)

extension (response: Response[IO])
  def getHeaderValue(headerName: CIString): Option[String] =
    getValue(headerName, response.headers)
