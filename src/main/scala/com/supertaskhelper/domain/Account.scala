package com.supertaskhelper.domain

import com.supertaskhelper.common.util.ValidatorUtil
import com.supertaskhelper.service.PaymentService
import spray.json._

case class Account(userId: String,
    email: String,
    mobile: Option[String],
    sms: Boolean,
    okForEmailsAboutBids: Boolean,
    okForEmailsAboutComments: Boolean,
    password: String,
    newPassword: Option[String],
    hasFacebookConnection: Boolean,
    paypalEmail: Option[String]) {

  require(!email.isEmpty && ValidatorUtil.isValidEmail(email), "email cannot be empty")
  require(!password.isEmpty, "password cannot be empty")
  require(paypalEmail.isEmpty || PaymentService.isPaypalEmailValid(paypalEmail.get), "paypal email not valid")
}

object AccountJsonFormat extends DefaultJsonProtocol {

  implicit val accountFormat = jsonFormat10(Account)
}
