package async.service

import cats.effect.IO

import java.time.Instant
import java.util.UUID

object JobProcessor {
  case class Job(count: Int, id: UUID)
}

class JobProcessor {
  
  import JobProcessor.*
  
  def prepare(from: Instant, to: Instant): IO[Job] = IO.raiseError(???)
  def process(from: Instant, to: Instant): IO[Unit] = IO.raiseError(???)
}
