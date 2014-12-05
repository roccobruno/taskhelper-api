package com.supertaskhelper.domain

import java.text.SimpleDateFormat
import java.util.Date

import spray.json._

case class Payment(id: String,
    created_date: Option[Date],
    taskId: String,
    userId: String,
    taskHelperId: String,
    authId: String,
    amount: String,
    currency: String,
    amountForSth: String,
    status: Option[String], // check TASK_STATUS
    paymentType: Option[String],
    capturedDate: Option[Date]) //check PAYMENT_TYPE
    {

  require(!id.isEmpty, "id cannot be empty. It is the Paypal payment Id")
  require(!taskId.isEmpty, "taskId cannot be empty")
  require(!userId.isEmpty, "userId cannot be empty")
  require(!taskHelperId.isEmpty, "taskHelperId cannot be empty")
  require(!authId.isEmpty, "authId cannot be empty")
  require(BigDecimal(amount) != 0, "amount cannot be 0")
  require(!currency.isEmpty, "currency cannot be empty")
  require(BigDecimal(amountForSth) != 0, "amountForSth cannot be 0")

}

object PaymentJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val paymentFormat = jsonFormat12(Payment)
}

