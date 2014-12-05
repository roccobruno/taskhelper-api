package com.supertaskhelper.service

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.MongoFactory
import com.supertaskhelper.common.util.Password
import com.supertaskhelper.domain.Account

trait AccountService extends Service with UserService with PaymentService {

  def buildAccount(t: DBObject): Option[Account] = {
    val account = Account(
      userId = t.getAs[ObjectId]("_id").get.toString,
      email = t.getAs[String]("email").get,
      mobile = t.getAs[String]("mobile"),
      sms = t.getAs[Boolean]("okForSMS").getOrElse(false),
      okForEmailsAboutBids = t.getAs[Boolean]("okForEmailsAboutBids").getOrElse(false),
      okForEmailsAboutComments = t.getAs[Boolean]("okForEmailsAboutComments").getOrElse(false),
      password = "NOT_VISIBLE",
      newPassword = Some("NONE"),
      hasFacebookConnection = t.getAs[Boolean]("hasFacebookConnection").getOrElse(false),
      paypalEmail = t.getAs[String]("paypalEmail")

    )

    Option(account)

  }

  def findAccount(userId: String): Option[Account] = {

    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(userId))
    val result = collection findOne q
    if (result != None) {
      val paymResult = result.get
      buildAccount(paymResult)

    } else
      None
  }

  def saveAccount(account: Account) = {

    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(account.userId))

    var setdata = $set("password" -> Password.getSaltedHash(account.password),
      "okForSMS" -> account.sms,
      "paypalEmail" -> account.paypalEmail,

      "okForEmailsAboutComments" -> account.okForEmailsAboutComments,
      "okForEmailsAboutBids" -> account.okForEmailsAboutBids,
      "email" -> account.email)

    val user = findUserById(account.userId)._2
    if (!user.isSTH) {
      setdata = $set("password" -> Password.getSaltedHash(account.password),
        "okForSMS" -> account.sms,
        "paypalEmail" -> account.paypalEmail,
        "mobile" -> account.mobile,
        "okForEmailsAboutComments" -> account.okForEmailsAboutComments,
        "okForEmailsAboutBids" -> account.okForEmailsAboutBids,
        "email" -> account.email)
    }

    collection update (q, setdata)

  }

  def checkPaypalEmail(paypalEmail: String) = {
    isPaypalEmailValid(paypalEmail)
  }

}
