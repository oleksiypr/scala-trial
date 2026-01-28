package async.service

import cats.effect.IO
import java.time.Instant

class JobProcessor {
  
  def count(from: Instant, to: Instant): IO[Int] = IO.raiseError(???)
}
