package com.supertaskhelper.service

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import com.supertaskhelper.common.service.paypal.PayPalServiceException
import com.supertaskhelper.domain.PaymentJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.{Payment, Response}
import com.supertaskhelper.service.PaymentServiceActor.{CapturePayment, DeletePayment, FindPayment, TransferPayment}
import com.supertaskhelper.util.ActorFactory
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.RequestContext

class PaymentServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
    with UserService with PaymentService {

  def receive = LoggingReceive {

    case p: FindPayment => {

      val payment: Option[Payment] = findPayment(p.id)
      if (payment.isEmpty)
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
      else
        httpRequestContext.complete(payment)

      context.stop(self)
    }

    case payment: Payment => {
      val response = savePayment(payment)
      httpRequestContext.complete(response)
      context.stop(self)
    }

    case d: DeletePayment => {
      val response = deletePayment(d.id)
      httpRequestContext.complete(response)
      context.stop(self)
    }

    case capture: CapturePayment => {

      try {
        PaymentService.capturePayment(capture.taskId)
        httpRequestContext.complete(Response("Success", "1"))
      } catch {
        case paypalExc: PayPalServiceException => {
          log.error(s"Error in capturing the payment for taskId ${capture.taskId}")
          log.error(s"Error in capturing paypal service error ${paypalExc.getMessage}")
          httpRequestContext.complete(StatusCodes.InternalServerError)
        }
        case e: Throwable => {
          log.error(s"Error in capturing the payment for taskId ${capture.taskId}")
          httpRequestContext.complete(StatusCodes.InternalServerError)
        }

      }

      context.stop(self)

    }

    case transfer: TransferPayment => {

      try {
        transferMoneyForSTH(transfer.sthId, transfer.amount, transfer.currency, transfer.paypalEmail)
        httpRequestContext.complete(Response("Success", "1"))
      } catch {
        case paypalExc: PayPalServiceException => {
          log.error(s"Error in transfering the payment for sthID ${transfer.sthId}")
          log.error(s"Error in transfering paypal service error ${paypalExc.getMessage}")
          httpRequestContext.complete(StatusCodes.InternalServerError)
        }
        case e: Throwable => {
          log.error(s"Error in transfering the payment for sthId ${transfer.sthId}")
          log.error(s"Error in transfering paypal service error ${e.getMessage}")
          httpRequestContext.complete(StatusCodes.InternalServerError)
        }

      }

      context.stop(self)

    }

  }
}

object PaymentServiceActor {

  case class FindPayment(id: String)
  case class DeletePayment(id: String)
  case class TransferPayment(sthId: String, amount: BigDecimal, currency: String, paypalEmail: String) {
    require(!sthId.isEmpty, "sthId cannot be empty")
    require(amount != 0, "amount cannot be 0")
    require(!currency.isEmpty, "currency cannot be empty")
    require(!paypalEmail.isEmpty, "paypalEmail cannot be empty")
  }
  case class CapturePayment(taskId: String) {
    require(!taskId.isEmpty, "taskId is missing")
  }

  object TransferPaymentFormat extends DefaultJsonProtocol {
    implicit val transferPaymentFormat = jsonFormat4(TransferPayment)
  }

  object CapturePaymentFormat extends DefaultJsonProtocol {
    implicit val capturePaymentFormat = jsonFormat1(CapturePayment)
  }
}