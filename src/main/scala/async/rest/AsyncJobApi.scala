package async.rest

import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import cats.effect.IO
import org.typelevel.ci.CIString
import org.typelevel.ci.*

object AsyncJobApi {
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case HEAD -> Root / "jobs" =>
      Accepted().map(_.putHeaders(Header.Raw(ci"X-Total-Count", "42")))
  }
}
