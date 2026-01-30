package async.service

import async.model.TimeRange
import cats.effect.IO
import java.time.Instant
import java.util.UUID

object JobService {
  case class Job(id: UUID, count: Int)
}

class JobService {
  
  import JobService.*
  
  def prepare(query: TimeRange): IO[Job] = IO.raiseError(???)
  def process(from: Instant, to: Instant): IO[Unit] = IO.raiseError(???)
}
