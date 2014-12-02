package com.supertaskhelper.service.paypal

import java.math

import com.paypal.core.rest.OAuthTokenCredential
import com.supertaskhelper.common.domain.Payment
import com.supertaskhelper.common.service.paypal.{IPayPalService, OAuthTokenCredentialWrapper, PayPalServiceImpl, PaymentApprovalResponse}
import com.supertaskhelper.service.PaymentServiceImpl
import com.typesafe.config.ConfigFactory

class PaypalServiceImplWrapper extends IPayPalService {
  override def getToken: String = {
    PaypalServiceImplWrapper.getToken()
  }

  override def isValidEmailWithAccount(paypalEmail: String): Boolean = ???

  override def refundPayment(taskId: String, amountToRefund: math.BigDecimal, currency: String): Boolean = ???

  override def reauthorizedPayment(payment: Payment): Boolean = ???

  override def executePaymentRequest(payerId: String, taskId: String, userId: String): Boolean = ???

  override def transferFundToSth(sthId: String, amount: math.BigDecimal, currency: String, paypalEmail: String): Boolean = {
    PaypalServiceImplWrapper.transferFundToSth(sthId, amount, currency, paypalEmail)
  }

  override def capturePayment(taskId: String): Boolean = {
    PaypalServiceImplWrapper.capturePayment(taskId)
  }

  override def makePaymentApprovalRequest(redirectUrl: String, cancelUrl: String, amount: math.BigDecimal, currency: String, taskId: String, sthId: String, amountToPayToTheSth: math.BigDecimal): PaymentApprovalResponse = ???
}

object PaypalServiceImplWrapper {
  def transferFundToSth(sthID: String, amountToTransfer: math.BigDecimal, currency: String, paypalEmail: String): Boolean = {
    iPaypalService.transferFundToSth(sthID, amountToTransfer, currency, paypalEmail)
  }

  val iPaypalService = new PayPalServiceImpl()
  iPaypalService.setApi_account_endPoint(ConfigFactory.load().getString("api-sth.paypal.service.api_account_endPoint"))
  iPaypalService.setApi_password(ConfigFactory.load().getString("api-sth.paypal.service.api_password"))
  iPaypalService.setApi_payment_endPoint(ConfigFactory.load().getString("api-sth.paypal.service.api_payment_endPoint"))
  iPaypalService.setApi_signature(ConfigFactory.load().getString("api-sth.paypal.service.api_signature"))
  iPaypalService.setApi_username(ConfigFactory.load().getString("api-sth.paypal.service.api_username"))
  iPaypalService.setApp_id(ConfigFactory.load().getString("api-sth.paypal.service.app_id"))
  iPaypalService.setClient_id(ConfigFactory.load().getString("api-sth.paypal.service.client_id"))
  iPaypalService.setSecret(ConfigFactory.load().getString("api-sth.paypal.service.secret"))
  iPaypalService.setPaymentService(new PaymentServiceImpl())
  iPaypalService.setRestTemplate(new org.springframework.web.client.RestTemplate())

  val tokenCredential = new OAuthTokenCredentialWrapper()
  tokenCredential.setoAuthTokenCredential(new OAuthTokenCredential(ConfigFactory.load().getString("api-sth.paypal.token.clientID"),
    ConfigFactory.load().getString("api-sth.paypal.token.clientSecret")))

  def getToken(): String = {
    iPaypalService.getTokenCredential.getAccessToken
  }

  iPaypalService.setTokenCredential(tokenCredential)

  def capturePayment(taskId: String): Boolean = {
    iPaypalService.capturePayment(taskId)
  }

}
