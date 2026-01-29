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
import scala.util.Try

object AsyncJobApi {

  given QueryParamDecoder[Instant] = QueryParamDecoder[String]
    .emap { s =>
      Try(Instant.parse(s))
        .toEither
        .leftMap(ex => ParseFailure(s, ex.getMessage))
    }

  private object FromParam extends QueryParamDecoderMatcher[Instant]("from")
  private object ToParam extends QueryParamDecoderMatcher[Instant]("to")
}

class AsyncJobApi(jobProcessor: JobProcessor) {

  import AsyncJobApi.*

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case HEAD -> Root / "jobs" :? FromParam(from) +& ToParam(to) =>
      jobProcessor.count(from, to) >>= { count =>
        Accepted().map(_.putHeaders(Header.Raw(ci"X-Total-Count", count.toString)))
      }
  }
}

