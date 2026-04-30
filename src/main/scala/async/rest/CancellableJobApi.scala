package async.rest

import async.service.CancellableService
import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.*

class CancellableJobApi(service: CancellableService) {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case POST -> Root / "jobs" =>
      for
        started <- service.start()
        resp    <- Accepted()
      yield
        resp
          .putHeader(Location(uri"/jobs" / started.id.toString))
          .putHeader(`X-Job-Id`(started.id))
          .putHeader(`X-Total-Count`(started.totalCount))

    case DELETE -> Root / "jobs" / UUIDVar(jobId) =>
      service.cancel(jobId).flatMap {
        case None => NotFound()
        case Some(snapshot) =>
          NoContent().map(
            _.putHeader(`X-Job-Id`(snapshot.id))
              .putHeader(`X-Done-Count`(snapshot.doneCount))
              .putHeader(`X-Job-Status`(snapshot.status.asHeader))
          )
      }

    case HEAD -> Root / "jobs" / UUIDVar(jobId) =>
      service.status(jobId).flatMap {
        case None => NotFound()
        case Some(snapshot) =>
          Ok().map { response =>
            val withCommon = response
              .putHeader(`X-Job-Id`(snapshot.id))
              .putHeader(`X-Done-Count`(snapshot.doneCount))
              .putHeader(`X-Job-Status`(snapshot.status.asHeader))

            snapshot.failureReason match {
              case Some(reason) => withCommon.putHeader(`X-Failure-Reason`(reason))
              case None         => withCommon
            }
          }
      }
}

