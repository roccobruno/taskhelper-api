package com.supertaskhelper.api

import java.util.Date

import com.supertaskhelper.common.enums.{TASK_STATUS, TASK_TYPE}
import com.supertaskhelper.domain.PaymentJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain._
import com.supertaskhelper.router.RouteHttpService
import com.supertaskhelper.service.PaymentServiceActor.TransferPayment
import com.supertaskhelper.service.PaymentServiceActor.TransferPaymentFormat._
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
    val taskId = "5454dde50364b8976342f1dd"

    "return payment with id" in {
      Get("/api/payments/" + payId) ~> route ~> check {
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

    val payment = Payment("PAY-2FX913169V0229117KRTXFIY", Option(new java.util.Date()), "5454dde50364b8976342f1dd", "52cd1539e4b041f92f2b2b17",
      "53977fd5e4b0acaff9dc2b54", "70731830JN634773L", "30", "USD", "23", None, None, Option(new java.util.Date()))

    var payIdNew = "PAY-2FX913169V0229117KRTXFIY"

    "should save payment " in {
      Post("/api/payments", payment) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resourse Added"))

      }

      Get("/api/payments/" + payIdNew) ~> route ~> check {
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
      Delete("/api/payments/" + payIdNew) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Deleted"))

      }
    }

    "should not return a payment with id" in {
      Get("/api/payments/" + payIdNew) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    val feedback = Feedback("5383766d036457876c3dc24f","feedback test",new Date(),5,"5454dde50364b8976342f1dd",
      Some("52cd1539e4b041f92f2b2b17"),Some("it"))
    //test close task
    import com.supertaskhelper.domain.FeedbackJsonFormat._

    "should capture a payment" in {
      Post("/api/payments/capture", feedback) ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "should transferMoney " in {
      Post("/api/payments/transfer", TransferPayment("53977fd5e4b0acaff9dc2b54", 0.01, "EUR", "roccobruno13@googlemail.com")) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }





    "it should update Task status when payment is posted" in {
      //need to check that when a payment is made the related task is updated correctly
      val taskPrice = TaskPrice(Option(true), Option("10"), Option(true), Option(2), Option(true), Option(3), Option("2"), Option("3"))
      val taskBadge = TaskBadges(Option(true), Option(true), Option(true), Option(true), Option(true), Option(true), Option(true))

      val task = Task(None,
        "Api Task Test",
        "Api Task test desc",
        new Date(),
        None,
        new Date(),
        "17.00",
        TASK_STATUS.TOAPPROVEREQUEST.toString,
        "53028f49036462126f7f042b",
        None,
        None,
        None,
        Option("Tuttofare"),
        Option("52515bb0e4b094388a43ca39"),
        TASK_TYPE.OFFLINE.toString,
        Option(taskBadge),
        "WITH_AUCTION_ONLY",
        Option("52515bb0e4b094388a43ca39"),
        Option(taskPrice),
        Option(true),
        None,
        Some("EUR")

      )
      import com.supertaskhelper.domain.TasksJsonFormat._
      var tId: String = ""
      Post("/api/tasks", task) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
        tId = responseAs[Response].id

      }

      val updateTaskStatus = UpdateTaskStatusParams(tId,TASK_STATUS.REQUESTACCEPTED.toString,Some("it"))

      import com.supertaskhelper.domain.UpdateTaskStatusParamsFormat._
      Post("/api/tasks/update",updateTaskStatus) ~> route ~> check {

        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))


      }

      var payIdNew1 = "PAY-3FX913169V0229117KRTXFIY"

      val payment2 = Payment(payIdNew1, Option(new java.util.Date()), tId, "52cd1539e4b041f92f2b2b17",
        "53977fd5e4b0acaff9dc2b54", "70731830JN634773L", "30", "USD", "23", None, None, Option(new java.util.Date()))

      Post("/api/payments", payment2) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resourse Added"))

      }



      import com.supertaskhelper.domain.TasksJsonFormat._
      Get("/api/tasks?id=" + tId) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Tasks].tasks(0).id.get.toString == tId)
        assert(responseAs[Tasks].tasks(0).status == TASK_STATUS.ASSIGNED.toString)
        assert(responseAs[Tasks].tasks(0).bids.isDefined)
        assert(responseAs[Tasks].tasks(0).bids.get.size ==1)

      }

      Delete("/api/payments/" + payIdNew1) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Deleted"))

      }

      Delete("/api/tasks?id=" + tId) ~> route ~> check {
        status should be(StatusCodes.OK)


      }
    }


  }
}
