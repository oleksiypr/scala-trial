package async.rest

import async.model.TimeRange
import async.service.JobService
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.generic.semiauto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.*

import java.time.Instant

object AsyncJobApi {
  given Decoder[TimeRange] = deriveDecoder[TimeRange]
  given EntityDecoder[IO, TimeRange] = jsonOf[IO, TimeRange]
}


class AsyncJobApi(jobService: JobService) {

  import AsyncJobApi.given 

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case req @ POST -> Root / "jobs" =>
      req.as[TimeRange] >>= { req =>
        for
          job  <- jobService.prepare(req.from, req.to)
          _    <- jobService.process(req.from, req.to).start
          resp <- Accepted()
        yield {
          resp
            .putHeader(Location(uri"/jobs" / job.id.toString))
            .putHeader(`X-Total-Count`(job.count))
        }
      }
}
