package async.common

import cats.effect.IO

trait Logger {
  def info(msg: String): IO[Unit]
}
