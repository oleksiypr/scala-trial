package async.service

import cats.effect.IO
import java.util.UUID
import scala.collection.concurrent.TrieMap

object CancellableService {

  case class JobStarted(id: UUID, totalCount: Long)

  enum JobStatus {
    case Running, Cancelled, Failed, Completed

    def asHeader: String = toString.toLowerCase
  }

  case class JobSnapshot(
      id: UUID,
      doneCount: Long,
      status: JobStatus,
      failureReason: Option[String] = None
  )
}

class CancellableService {

  import CancellableService.*

  def start(): IO[JobStarted] = IO.pure(JobStarted(UUID.randomUUID(), totalCount = 100L))
  def cancel(jobId: UUID): IO[Option[JobSnapshot]] = IO.pure(None)
  def status(jobId: UUID): IO[Option[JobSnapshot]] = IO.pure(None)
}
