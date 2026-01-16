package async.rest

import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import cats.effect.IO

object AsyncJobApi {
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case HEAD -> Root / "jobs" => Accepted()
  }
}
