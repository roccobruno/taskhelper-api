package com.supertaskhelper.api

import com.supertaskhelper.domain.PaymentJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.{Payment, Response}
import com.supertaskhelper.router.RouteHttpService
import com.supertaskhelper.service.PaymentServiceActor.CapturePaymentFormat._
import com.supertaskhelper.service.PaymentServiceActor.TransferPaymentFormat._
import com.supertaskhelper.service.PaymentServiceActor.{CapturePayment, TransferPayment}
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._



/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecPayments extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(25 seconds)




  def actorRefFactory = system

  "The API Service" should {


    val payId = "PAY-1FX913169V0229117KRTXFIY"
    val taskId="5454dde50364b8976342f1dd"

     "return payment with id" in {
       Get("/api/payments/"+payId) ~> route ~> check {
         responseAs[Payment].id == payId
         responseAs[Payment].taskId == taskId
         responseAs[Payment].status == "completed"
         responseAs[Payment].paymentType == "RECEIVED"
         responseAs[Payment].authId == "70731830JN634772L"
         responseAs[Payment].amount == "26"
         responseAs[Payment].amountForSth == "22"
         responseAs[Payment].currency == "22"
         responseAs[Payment].userId == "52cd1539e4b041f92f2b2b17"
         responseAs[Payment].taskHelperId == "53977fd5e4b0acaff9dc2b54"
       }
     }

    val payment = Payment("PAY-2FX913169V0229117KRTXFIY",Option(new java.util.Date()),"5454dde50364b8976342f1dd","52cd1539e4b041f92f2b2b17",
    "53977fd5e4b0acaff9dc2b54","70731830JN634773L","30","USD","23",None,None)

    var payIdNew = "PAY-2FX913169V0229117KRTXFIY"

    "should save payment " in {
      Post("/api/payments",payment) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resourse Added"))

      }

      Get("/api/payments/"+payIdNew) ~> route ~> check {
        responseAs[Payment].id == payIdNew
        responseAs[Payment].taskId == "5454dde50364b8976342f1dd"
        responseAs[Payment].status == "completed"
        responseAs[Payment].paymentType == "RECEIVED"
        responseAs[Payment].authId == "70731830JN634773L"
        responseAs[Payment].amount == "30"
        responseAs[Payment].amountForSth == "23"
        responseAs[Payment].currency == "22"
        responseAs[Payment].userId == "52cd1539e4b041f92f2b2b17"
        responseAs[Payment].taskHelperId == "53977fd5e4b0acaff9dc2b54"
      }
    }



    "should delete a payment " in {
      Delete("/api/payments/"+payIdNew) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Deleted"))

      }
    }

    "should not return a payment with id" in {
      Get("/api/payments/" + payIdNew) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "should capture a payment" in {
      Post("/api/payments/capture",CapturePayment(taskId)) ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "should transferMoney " in {
      Post("/api/payments/transfer",TransferPayment("53977fd5e4b0acaff9dc2b54",0.01,"EUR","roccobruno13@googlemail.com")) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }



  }
}
