import java.util.UUID

import scala.util.{Failure, Success, Try}
import Service._

class App {

  case class Response(status: Int, body: String)

  val x: Response = Response(status = 200, "hi").copy(body = "hi, vasy")

  sealed trait Failed
  case class ClientApiFailed(message: String) extends Failed
  case class Forbidden(meessage: String) extends Failed
  case class Unknown(message: String) extends Failed

  class SafeService {

    private val service = new Service

    def deleteAccountPaymentMethod(
        accountId: UUID, paymentMethodId: UUID): Either[Failed, String]  = {

      Try {
        service.deleteAccountPaymentMethod(accountId, paymentMethodId)
      } match {
        case Success(_) => Right("Delete succeed")
        case Failure(ex) => ex match {
          case ex: HttpForbiddenException => Left(Forbidden(ex.getMessage))
          case ex: KillBillClientException => Left(ClientApiFailed(ex.getMessage))
          case ex: Throwable =>  Left(Unknown(ex.getMessage))
        }
      }
    }
  }


  class Controller {

    val safeService = new SafeService

    val eh: Failed => Response = {
      case ClientApiFailed(message) => Response(status = 503, message)
      case Forbidden(message) => Response(status = 403, message)
      case Unknown(message) => Response(status = 500, message)
    }

    def get(accountId: UUID, paymentMethodId: UUID): Response = {
      val res = safeService.deleteAccountPaymentMethod(accountId, paymentMethodId)
      res match {
        case Right(value) => Response(status = 200, body = value)
        case Left(issue) => eh(issue)
      }
    }

    def foo(accountId: UUID, paymentMethodId: UUID)(f: String => Int): Either[Failed, Int] = {
      safeService.deleteAccountPaymentMethod(accountId, paymentMethodId).map(f)
    }
  }
}
