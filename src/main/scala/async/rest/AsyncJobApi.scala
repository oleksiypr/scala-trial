package async.rest

import async.service.JobProcessor
import cats.effect.IO
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.typelevel.ci.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.util.Try

object AsyncJobApi {
  
  private val CountHeader = ci"X-Total-Count"

  given QueryParamEncoder[Instant] = QueryParamEncoder[String]
    .contramap(DateTimeFormatter.ISO_INSTANT.format)

  given QueryParamDecoder[Instant] = QueryParamDecoder[String]
    .emap : s =>
      Try(Instant.parse(s))
        .toEither
        .leftMap(ex => ParseFailure(s, ex.getMessage))
    

  private object FromParam extends QueryParamDecoderMatcher[Instant]("from")
  private object ToParam extends QueryParamDecoderMatcher[Instant]("to")
}

class AsyncJobApi(jobProcessor: JobProcessor) {

  import AsyncJobApi.*

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case HEAD -> Root / "jobs" :? FromParam(from) +& ToParam(to) =>
      jobProcessor.count(from, to) >>= { count =>
        Accepted().putHeader(CountHeader, count.toString)
      }
  }
}

