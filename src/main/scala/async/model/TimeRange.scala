package async.model

import java.time.Instant
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import cats.effect.IO

case class TimeRange(from: Instant, to: Instant)

