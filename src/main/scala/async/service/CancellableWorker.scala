package async.service

import cats.effect.IO
import java.util.UUID

trait CancellableWorker {
  def prepare(): IO[(jobId: UUID, totalCount: Long)]
  def run(jobId: UUID): IO[Unit]
}

