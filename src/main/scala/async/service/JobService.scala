package async.service

import async.common.TimeRange
import cats.effect.IO
import java.util.UUID

object JobService {
  case class Job(id: UUID, count: Long, query: TimeRange)
}

class JobService {

  import JobService.*

  def prepare(query: TimeRange): IO[Job] = IO.raiseError(???)
  def process(job: Job): IO[Unit] = IO.raiseError(???)
}
