package async.rest

import async.service.JobProcessor
import cats.effect.IO
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.generic.semiauto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.typelevel.ci.*
import java.time.Instant

object AsyncJobApi {

  case class JobRequest(from: Instant, to: Instant)

  given Decoder[JobRequest] = deriveDecoder[JobRequest]
  given EntityDecoder[IO, JobRequest] = jsonOf[IO, JobRequest]

  private val CountHeader = ci"X-Total-Count"
}

class AsyncJobApi(jobProcessor: JobProcessor) {

  import AsyncJobApi.*

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "jobs" =>
      req.as[JobRequest].flatMap { jobQuery =>
        jobProcessor.prepare(jobQuery.from, jobQuery.to) >>= { job =>
          Accepted().putHeader(CountHeader, job.count.toString)
        }
      }
  }
}
