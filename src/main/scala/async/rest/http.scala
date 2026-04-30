package async.rest

import cats.effect.IO
import org.http4s.{Header, ParseResult, Response}
import org.typelevel.ci.*
import java.util.UUID

final case class `X-Total-Count`(count: Long)

object `X-Total-Count` {
  given Header[`X-Total-Count`, Header.Single] =
    Header.create(
      ci"X-Total-Count",
      _.count.toString,
      s => ParseResult.fromTryCatchNonFatal("Invalid X-Total-Count")(
        `X-Total-Count`(s.toLong)
      )
    )
}

final case class `X-Job-Id`(jobId: UUID)

object `X-Job-Id` {
  given Header[`X-Job-Id`, Header.Single] =
    Header.create(
      ci"X-Job-Id",
      _.jobId.toString,
      s => ParseResult.fromTryCatchNonFatal("Invalid X-Job-Id")(`X-Job-Id`(UUID.fromString(s)))
    )
}

final case class `X-Done-Count`(count: Long)

object `X-Done-Count` {
  given Header[`X-Done-Count`, Header.Single] =
    Header.create(
      ci"X-Done-Count",
      _.count.toString,
      s => ParseResult.fromTryCatchNonFatal("Invalid X-Done-Count")(`X-Done-Count`(s.toLong))
    )
}

final case class `X-Job-Status`(status: String)

object `X-Job-Status` {
  given Header[`X-Job-Status`, Header.Single] =
    Header.create(ci"X-Job-Status", _.status, s => ParseResult.success(`X-Job-Status`(s)))
}

final case class `X-Failure-Reason`(reason: String)

object `X-Failure-Reason` {
  given Header[`X-Failure-Reason`, Header.Single] =
    Header.create(ci"X-Failure-Reason", _.reason, s => ParseResult.success(`X-Failure-Reason`(s)))
}

extension (response: Response[IO])
  def putHeader[T: [t] =>> Header[t, ?]](header: T): Response[IO] =
    response.putHeaders(header)

