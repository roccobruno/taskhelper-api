package com.supertaskhelper.domain

import spray.json._

/*



private   var email : String = null


private   var mobile : String = null


private   var sms : Boolean = false


private   var okForEmailsAboutBids : Boolean = false


private   var okForEmailsAboutComments : Boolean = false


private   var password : String = null


private   var newPassword : String = null


private   var hasFacebookConnection : Boolean = false


private   var paypalEmail : String = null
 */
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

  require(!email.isEmpty, "email cannot be empty")
  require(!password.isEmpty, "password cannot be empty")
}

object AccountJsonFormat extends DefaultJsonProtocol {

  implicit val accountFormat = jsonFormat10(Account)
}
