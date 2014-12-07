package com.supertaskhelper.service

import java.util.Date
import java.{math, util}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.MongoFactory
import com.supertaskhelper.common.domain.Task
import com.supertaskhelper.common.enums.{PAYMENT_STATUS, PAYMENT_TYPE}
import com.supertaskhelper.common.service.IPaymentService
import com.supertaskhelper.domain.Response
import com.supertaskhelper.service.paypal.PaypalServiceImplWrapper
import com.supertaskhelper.util.ConverterUtil
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions

trait PaymentService extends Service with ConverterUtil with IPaymentService {

  val logger = LoggerFactory.getLogger(classOf[PaymentService])

  val conn = MongoFactory.getConnection

  def buildPayment(t: DBObject): Option[com.supertaskhelper.domain.Payment] = {

    val payment = com.supertaskhelper.domain.Payment(
      id = t.getAs[String]("_id").get,
      created_date = t.getAs[java.util.Date]("created_date"),
      capturedDate = t.getAs[java.util.Date]("capturedDate"),
      taskId = t.getAs[String]("taskId").get,
      userId = t.getAs[String]("userId").get,
      taskHelperId = t.getAs[String]("taskHelperId").get,
      authId = t.getAs[String]("authId").get,
      amount = t.getAs[String]("amount").get,
      currency = t.getAs[String]("currency").get,
      amountForSth = t.getAs[String]("amountForSth").get,
      status = t.getAs[String]("status"),
      paymentType = t.getAs[String]("type")
    )
    Option(payment)
  }

  def buildDiffPayment(t: DBObject): com.supertaskhelper.common.domain.Payment = {



    var paym = new com.supertaskhelper.common.domain.Payment();
    paym.setAmount(new java.math.BigDecimal(t.getAs[String]("amount").getOrElse("0")))
    paym.setAmountForSth(new java.math.BigDecimal(t.getAs[String]("amountForSth").getOrElse("0")))
    paym.setAuthId(t.getAs[String]("authId").getOrElse(""))
    paym.setCreated_date(t.getAs[java.util.Date]("created_date").getOrElse(new Date()))
    paym.setCurrency(t.getAs[String]("currency").getOrElse(""))
    paym.setId(t.getAs[String]("_id").get)
    paym.setStatus(t.getAs[String]("status").getOrElse(""))
    paym.setTaskHelperId(t.getAs[String]("taskHelperId").getOrElse(""))
    paym.setTaskId(t.getAs[String]("taskId").getOrElse(""))
    paym.setType(t.getAs[String]("type").getOrElse(""))
    paym.setUserId(t.getAs[String]("userId").getOrElse(""))
    paym.setCapturedDate(t.getAs[java.util.Date]("capturedDate").getOrElse(new Date()))
    paym
  }

  def findPayment(id: String): Option[com.supertaskhelper.domain.Payment] = {

    val builder = MongoDBObject.newBuilder
    builder += "_id" -> id
    val collection = MongoFactory.getCollection("payment")
    val result = collection findOne builder.result
    if (result != None) {
      val paymResult = result.get
      buildPayment(paymResult)

    } else
      None
  }

  def findPaymentByTaskId(taskId: String): Option[com.supertaskhelper.domain.Payment] = {
    logger.debug("CIAO :" + taskId)

    val builder = MongoDBObject.newBuilder
    builder += "taskId" -> taskId
    val collection = MongoFactory.getCollection("payment")
    val result = collection findOne builder.result
    if (result != None) {
      val paymResult = result.get
      buildPayment(paymResult)

    } else
      None
  }

  def buildMongodbPaymentObj(payment: com.supertaskhelper.domain.Payment): MongoDBObject = {

    MongoDBObject(
      "_id" -> payment.id,
      "created_date" -> payment.created_date.getOrElse(new java.util.Date()),
      "taskId" -> payment.taskId,
      "userId" -> payment.userId,
      "taskHelperId" -> payment.taskHelperId,
      "authId" -> payment.authId,
      "amount" -> payment.amount,
      "currency" -> payment.currency,
      "amountForSth" -> payment.amountForSth,
      "status" -> PAYMENT_STATUS.approved.toString,
      "type" -> PAYMENT_TYPE.RECEIVED.toString
    )

  }

  def deletePayment(id: String): Response = {
    val builder = MongoDBObject.newBuilder
    builder += "_id" -> id
    val collection = MongoFactory.getCollection("payment")
    collection remove builder.result
    Response("Resource Deleted", "1")
  }

  def savePayment(payment: com.supertaskhelper.domain.Payment): Response = {

    val collection = MongoFactory.getCollection("payment")
    collection save buildMongodbPaymentObj(payment)
    Response("Resourse Added", "1")
  }

  def getPaymentByTaskId(taskId: String): com.supertaskhelper.common.domain.Payment = {
    val paym = findPaymentByTaskId(taskId);

    val payment = new com.supertaskhelper.common.domain.Payment()
    payment.setAuthId(paym.get.authId)
    payment.setCurrency(paym.get.currency)
    payment.setAmount(new java.math.BigDecimal(paym.get.amount))
    payment.setId(paym.get.id)
    payment

  }

  def findPaymentsBySthId(sthId: String): List[com.supertaskhelper.common.domain.Payment] = {
    val builder = MongoDBObject.newBuilder
    builder += "taskHelperId" -> sthId
    val collection = MongoFactory.getCollection("payment")
    val result = collection find builder.result

    if (result != None) {
      result.map(x => buildDiffPayment(x)).toList

    } else
      List()
  }

  def isPaypalEmailValid(paypalEmail: String): Boolean = {
    PaymentService.isPaypalEmailValid(paypalEmail)
  }

  def transferMoneyForSTH(sthId: String, amountToTransfer: BigDecimal, currency: String, paypalEmail: String) = {
    PaymentService.transferMoneyToSTH(new math.BigDecimal(amountToTransfer.toString()), currency, sthId, paypalEmail)
  }

  def updatePayment(payment: com.supertaskhelper.common.domain.Payment) = {

  }
  override def createNewPayment(id: String, taskId: String, state: String, `type`: PAYMENT_TYPE): Unit = ???

  override def updatePaymentWithNewState(taskId: String, state: String): Unit = ???

  override def getPaymentsBySTHId(taskhelperId: String, status: PAYMENT_STATUS, `type`: PAYMENT_TYPE): util.List[com.supertaskhelper.common.domain.Payment] = ???

  override def getPaymentsBySTHId(taskhelperId: String): util.List[com.supertaskhelper.common.domain.Payment] = {
    val builder = MongoDBObject.newBuilder
    builder += "taskHelperId" -> taskhelperId

    val collection = MongoFactory.getCollection("payment")
    val result = collection find builder.result

    val resultList: java.util.List[com.supertaskhelper.common.domain.Payment] = new util.ArrayList()
    resultList.addAll(JavaConversions.asJavaCollection(result.map(x => buildDiffPayment(x)).toList))
    resultList
  }

  override def updatePaymentWithPayerId(taskId: String, payerId: String): Unit = ???

  override def getAllTheNotReauthorizedApproved: util.List[com.supertaskhelper.common.domain.Payment] = ???

  override def getPaymentsByUserId(userId: String, status: PAYMENT_STATUS, `type`: PAYMENT_TYPE): util.List[com.supertaskhelper.common.domain.Payment] = {
    val builder = MongoDBObject.newBuilder
    builder += "userId" -> userId
    builder += "status" -> status.toString
    builder += "type" -> `type`.toString
    val collection = MongoFactory.getCollection("payment")
    val result = collection find builder.result

    val resultList: java.util.List[com.supertaskhelper.common.domain.Payment] = new util.ArrayList()
    resultList.addAll(JavaConversions.asJavaCollection(result.map(x => buildDiffPayment(x)).toList))
    resultList
  }

  override def getPaymentsByUserId(userId: String): util.List[com.supertaskhelper.common.domain.Payment] = ???

  override def updatePaymentsWithNewState(ids: util.List[String], status: String): Unit = ???

  override def isPaymentForThisTaskTransferred(task: Task): Boolean = ???

  override def getPayments(status: PAYMENT_STATUS, `type`: PAYMENT_TYPE): util.List[com.supertaskhelper.common.domain.Payment] = ???

  override def getClosedPayments(`type`: PAYMENT_TYPE): util.List[com.supertaskhelper.common.domain.Payment] = ???

}

object PaymentService {

  val iPaypalService = new PaypalServiceImplWrapper()

  def capturePayment(taskId: String): Unit = {

    iPaypalService.capturePayment(taskId)
  }

  def isPaypalEmailValid(paypalEmail: String): Boolean = {
    iPaypalService.isValidEmailWithAccount(paypalEmail)
  }

  def transferMoneyToSTH(amountToTransfer: java.math.BigDecimal, currency: String, sthId: String, paypalEmail: String): Unit = {
    iPaypalService.transferFundToSth(sthId, amountToTransfer, currency, paypalEmail)
  }

}
