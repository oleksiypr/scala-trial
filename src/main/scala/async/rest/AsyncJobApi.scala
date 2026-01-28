package async.rest

import async.service.JobProcessor
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import cats.effect.IO
import org.typelevel.ci.CIString
import org.typelevel.ci.*
import java.time.Instant
import cats.syntax.all.*


class AsyncJobApi(jobProcessor: JobProcessor) {
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case HEAD -> Root / "jobs" :? FromParam(from) +& ToParam(to) =>
      val fromInstant = Instant.parse(from)
      val toInstant = Instant.parse(to)
      jobProcessor.count(fromInstant, toInstant) >>= { count =>
        Accepted().map(_.putHeaders(Header.Raw(ci"X-Total-Count", count.toString)))
      }
  }
}

object FromParam extends QueryParamDecoderMatcher[String]("from")
object ToParam   extends QueryParamDecoderMatcher[String]("to")
