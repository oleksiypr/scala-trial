package async.service

import cats.effect.IO
import cats.syntax.all.*
import java.util.UUID

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

class CancellableService(worker: CancellableWorker) {

  import CancellableService.*

  def start(): IO[JobStarted] =
    for
      prep <- worker.prepare()
      _    <- worker.run(prep.jobId).start
    yield JobStarted(prep.jobId, prep.totalCount)

  def cancel(jobId: UUID): IO[Option[JobSnapshot]] = IO.pure(None)
  def status(jobId: UUID): IO[Option[JobSnapshot]] = IO.pure(None)
}
