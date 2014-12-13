package com.supertaskhelper.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import com.mongodb.casbah.Imports._
import com.supertaskhelper.common.enums.TASK_STATUS
import com.supertaskhelper.common.jms.alerts.{AssignedTaskAlert, ClosedTaskAlert}
import com.supertaskhelper.common.service.paypal.PayPalServiceException
import com.supertaskhelper.domain.PaymentJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.{Feedback, Payment, Response}
import com.supertaskhelper.service.PaymentServiceActor.{DeletePayment, FindPayment, TransferPayment}
import com.supertaskhelper.util.ActorFactory
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.RequestContext

class PaymentServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
    with UserService with PaymentService with TaskService with AlertMessageService {

  def actorRefFactory = context

  def createPerTaskActor(ctx: RequestContext): ActorRef = {

    actorRefFactory.actorOf(Props(classOf[TaskServiceActor], ctx))
  }

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

      assignTask(payment.taskId, payment.taskHelperId)

      val response = savePayment(payment)

      //send out alerts
      val alertActor = createSendAlertActor(context)
      alertActor ! new AssignedTaskAlert(payment.taskId, payment.taskHelperId)

      httpRequestContext.complete(response)
      context.stop(self)
    }

    case d: DeletePayment => {
      val response = deletePayment(d.id)
      httpRequestContext.complete(response)
      context.stop(self)
    }

    case capture: Feedback => {

      try {
        PaymentService.capturePayment(capture.taskId)

        val taskServiceActor = createPerTaskActor(httpRequestContext)
        taskServiceActor ! capture

        //need to update the task status and send out an alert
        updateTaskAttribute(capture.taskId, $set("status" -> TASK_STATUS.CLOSED.toString))
        //send out alert

        val alertActor = createSendAlertActor(context)
        alertActor ! new ClosedTaskAlert(capture.taskId, "IT")

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
