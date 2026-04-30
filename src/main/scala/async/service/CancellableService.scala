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

trait CancellableService {

  import CancellableService.*

  def start(): IO[JobStarted]
  def cancel(jobId: UUID): IO[Option[JobSnapshot]]
  def status(jobId: UUID): IO[Option[JobSnapshot]]
}

final class DummyCancellableService extends CancellableService {

  import CancellableService.*

  private val jobs = TrieMap.empty[UUID, JobSnapshot]

  override def start(): IO[JobStarted] = IO {
    val id      = UUID.randomUUID()
    val started = JobStarted(id = id, totalCount = 100L)
    jobs.update(id, JobSnapshot(id = id, doneCount = 0L, status = JobStatus.Running))
    started
  }

  override def cancel(jobId: UUID): IO[Option[JobSnapshot]] = IO {
    jobs.get(jobId).map {
      case snapshot if isTerminal(snapshot.status) => snapshot
      case snapshot =>
        val cancelled = snapshot.copy(status = JobStatus.Cancelled)
        jobs.update(jobId, cancelled)
        cancelled
    }
  }

  override def status(jobId: UUID): IO[Option[JobSnapshot]] =
    IO.pure(jobs.get(jobId))

  private def isTerminal(status: JobStatus): Boolean =
    status == JobStatus.Cancelled || status == JobStatus.Failed || status == JobStatus.Completed
}

