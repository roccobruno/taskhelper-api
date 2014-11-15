package com.supertaskhelper.api

import java.util.{Calendar, Date, GregorianCalendar, Locale}

import com.supertaskhelper.common.enums.{SOURCE, TASK_STATUS, TASK_TYPE}
import com.supertaskhelper.domain.CommentAnswerJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.domain.UpdateTaskStatusParamsFormat._
import com.supertaskhelper.domain.{Address, Location, Response, Task, _}
import com.supertaskhelper.router.RouteHttpService
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.ValidationRejection
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecTask extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(600 seconds)

  val location = Location("40.1508677", "16.2848214")
  val address = Address(Option("via carlo levi API"), Option("Senise"), "Italia", Option(location),
    Option("85038"), Option("Basilicata")
  )

  val taskPrice = TaskPrice(Option(true),Option("10"),Option(true),Option(2),Option(true),Option(3),Option("2"),Option("3"))
  val taskBadge = TaskBadges(Option(true), Option(true), Option(true), Option(true), Option(true), Option(true), Option(true))

  val task = Task(None,
    "Api Task Test",
    "Api Task test desc",
    new Date(),
    Option(address),
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
    Option(true)

  )



  def actorRefFactory = system

  "The API Service" should {

    var taskId: Option[String] = None

    "return the status message" in new RouteHttpSpecTask {
      Get("/api/status") ~> route ~> check {
        responseAs[String] should include("\"status\": \"API-STH is running\"")
      }
    }

    "accept request to add task to index" in {
      Post("/api/tasks", task) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
        taskId = Option(responseAs[Response].id)
        println(taskId)
      }


      val updateTaskStatus = UpdateTaskStatusParams(taskId.get,TASK_STATUS.REQUESTACCEPTED.toString,Some("it"))

      Post("/api/tasks/update",updateTaskStatus) ~> route ~> check {

        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))


      }





      val comment = Comment(None,"52515bb0e4b094388a43ca39","rocco","API test comment",new Date(),taskId.get,
        None,None,false)

      Post("/api/tasks/comments",comment) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      val comment2 = Comment(None,"52515bb0e4b094388a43ca39","rocco","API test comment",new Date(),"52515bb0e5b094388a43ca39",
        None,None,false)

//      Post("/api/tasks/comments",comment2) ~> route ~> check {
//        assert(rejections.size ==1)
//        assert(rejections(0).isInstanceOf[MalformedQueryParamRejection])
//        assert(rejections(0).asInstanceOf[MalformedQueryParamRejection].parameterName == "customerId")
//      }





      Post("/api/tasks/comments",comment2) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }

      val bid = Bid(None,"10","11","52515bb0e4b094388a43ca39","paolo","ciao",taskId,None,None)
      Post("/api/tasks/bids",bid) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      var commentIDTest:Option[String] = None

      import com.supertaskhelper.domain.TasksJsonFormat._
      Get("/api/tasks?id=" + taskId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Tasks].tasks(0).id == taskId)
        assert(responseAs[Tasks].tasks(0).title == task.title)
        assert(responseAs[Tasks].tasks(0).description == task.description)
        assert(responseAs[Tasks].tasks(0).time == task.time)
        assert(responseAs[Tasks].tasks(0).address.get == task.address.get)
        assert(responseAs[Tasks].tasks(0).address.get.address == task.address.get.address)
        assert(responseAs[Tasks].tasks(0).address.get.city == task.address.get.city)
        assert(responseAs[Tasks].tasks(0).address.get.postcode == task.address.get.postcode)
        assert(responseAs[Tasks].tasks(0).address.get.regione == task.address.get.regione)
        assert(responseAs[Tasks].tasks(0).address.get.country == task.address.get.country)
        assert(responseAs[Tasks].tasks(0).address.get.location == task.address.get.location)
        assert(responseAs[Tasks].tasks(0).address.get.location.get.latitude == task.address.get.location.get.latitude)
        assert(responseAs[Tasks].tasks(0).address.get.location.get.longitude == task.address.get.location.get.longitude)
        assert(responseAs[Tasks].tasks(0).category == task.categoryId)
        assert(responseAs[Tasks].tasks(0).categoryId == task.categoryId)
        assert(responseAs[Tasks].tasks(0).status == TASK_STATUS.REQUESTACCEPTED.toString)
        assert(responseAs[Tasks].tasks(0).badges.get.passportIdBudgetRequired == task.badges.get.passportIdBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.webcamBudgetRequired == task.badges.get.webcamBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.secDocBudgetRequired == task.badges.get.secDocBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.fbBudgetRequired == task.badges.get.fbBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.twitterBudgetRequired == task.badges.get.twitterBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.linkedInBudgetRequired == task.badges.get.linkedInBudgetRequired)
        assert(responseAs[Tasks].tasks(0).badges.get.emailVerBudgetRequired == task.badges.get.emailVerBudgetRequired)
        assert(responseAs[Tasks].tasks(0).requestType == task.requestType)
        assert(responseAs[Tasks].tasks(0).hireSthId.get == task.hireSthId.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.hasPriceSuggested.get == task.taskPrice.get.hasPriceSuggested.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.isPerHour.get == task.taskPrice.get.isPerHour.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.nOfHours.get == task.taskPrice.get.nOfHours.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.nOfWeeks.get == task.taskPrice.get.nOfWeeks.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.priceSuggested.get == task.taskPrice.get.priceSuggested.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.tariffWithFeeForSth.get == task.taskPrice.get.tariffWithFeeForSth.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.tariffWithoutFeeForSth.get == task.taskPrice.get.tariffWithoutFeeForSth.get)
          assert(responseAs[Tasks].tasks(0).taskPrice.get.toRepeat.get == task.taskPrice.get.toRepeat.get)
        assert(responseAs[Tasks].tasks(0).comments.isDefined)
        assert(responseAs[Tasks].tasks(0).comments.get(0).comment == comment.comment)
        assert(responseAs[Tasks].tasks(0).comments.get(0).taskId == comment.taskId)
        assert(responseAs[Tasks].tasks(0).comments.get(0).userId == comment.userId)
        assert(responseAs[Tasks].tasks(0).comments.get(0).userName == comment.userName)
        assert(responseAs[Tasks].tasks(0).comments.get(0).id.isDefined)
        assert(responseAs[Tasks].tasks(0).comments.get(0).dateCreated.before(new Date()))
        assert(responseAs[Tasks].tasks(0).comments.get(0).conversation == false)
        commentIDTest = responseAs[Tasks].tasks(0).comments.get(0).id
        assert(responseAs[Tasks].tasks(0).bids.isDefined)
        assert(responseAs[Tasks].tasks(0).bids.get(0).comment == bid.comment)
        assert(responseAs[Tasks].tasks(0).bids.get(0).taskId == bid.taskId)
        assert(responseAs[Tasks].tasks(0).bids.get(0).sth == bid.sth)
        assert(responseAs[Tasks].tasks(0).bids.get(0).sthId == bid.sthId)
        assert(responseAs[Tasks].tasks(0).bids.get(0).id.isDefined)
        assert(responseAs[Tasks].tasks(0).bids.get(0).offeredValue == bid.offeredValue)
        assert(responseAs[Tasks].tasks(0).bids.get(0).incrementedValue ==bid.incrementedValue)
        assert(responseAs[Tasks].tasks(0).bids.get(0).createdDate.isDefined)
        assert(responseAs[Tasks].tasks(0).doneBy == Option(true))
      }

      val date = new GregorianCalendar()
      date.add(Calendar.DAY_OF_MONTH,-1);
      val commentansw1 = CommentAnswer(None,"52515bb0e4b094388a43ca39","roccopippo","API test commentanswer",date.getTime,taskId.get,
        None,commentIDTest)

      val commentansw2 = CommentAnswer(None,"52515bb0e4b094388a43ca39","rocco","API test comment",new Date(),taskId.get,
        None,commentIDTest)

      Post("/api/tasks/comments/answers",commentansw1) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      Post("/api/tasks/comments/answers",commentansw2) ~> route ~> check {
        status should be(StatusCodes.OK)
      }


      import com.supertaskhelper.domain.CommentsJsonFormat._
      Get("/api/tasks/comments/answers?commentId="+commentIDTest.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Comments].comments.size == 2)
        assert(responseAs[Comments].comments(0).comment == commentansw2.comment)
        assert(responseAs[Comments].comments(0).taskId == commentansw2.taskId)
        assert(responseAs[Comments].comments(0).userId == commentansw2.userId)
        assert(responseAs[Comments].comments(0).userName == commentansw2.userName)
        assert(responseAs[Comments].comments(0).id.isDefined)
        assert(responseAs[Comments].comments(0).dateCreated.before(new Date()))
        assert(responseAs[Comments].comments(0).commentId== commentansw2.commentId)
        assert(responseAs[Comments].comments(0).conversation == false)
      }




      import com.supertaskhelper.domain.CommentsJsonFormat._
       Get("/api/tasks/comments?taskId="+taskId.get) ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Comments].comments.size == 1)
         assert(responseAs[Comments].comments(0).comment == comment.comment)
         assert(responseAs[Comments].comments(0).taskId == comment.taskId)
         assert(responseAs[Comments].comments(0).userId == comment.userId)
         assert(responseAs[Comments].comments(0).userName == comment.userName)
         assert(responseAs[Comments].comments(0).id.isDefined)
         assert(responseAs[Comments].comments(0).dateCreated.before(new Date()))
         assert(responseAs[Comments].comments(0).conversation == true)
       }

      Delete("/api/tasks/comments/answers?commentId="+commentIDTest.get)   ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      Get("/api/tasks/comments/answers?commentId="+commentIDTest.get) ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }

      import com.supertaskhelper.domain.BidsJsonFormat._
      Get("/api/tasks/bids?taskId="+taskId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Bids].bids.size == 1)
        assert(responseAs[Bids].bids(0).comment == bid.comment)
        assert(responseAs[Bids].bids(0).taskId == bid.taskId)
        assert(responseAs[Bids].bids(0).sthId == bid.sthId)
        assert(responseAs[Bids].bids(0).sth == bid.sth)
        assert(responseAs[Bids].bids(0).status == bid.status)
        assert(responseAs[Bids].bids(0).incrementedValue == bid.incrementedValue)
        assert(responseAs[Bids].bids(0).offeredValue == bid.offeredValue)

        assert(responseAs[Bids].bids(0).id.isDefined)
        assert(responseAs[Bids].bids(0).createdDate.get.before(new Date()))
      }

      val email:String = "test_rocco_reg@msn.com"
      val password:String = "test_rocco"
      val username = "test_rocco"
      val userReg = UserRegistration(username,"test_lastname",password,"test_rocco",email,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))
      var userId: Option[String] = None
      import com.supertaskhelper.domain.UserRegistrationJsonFormat._
      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
      }

      val feedback = Feedback("5383766d036457876c3dc24f","feedback test",new Date(),5,taskId.get,
        userId,Some("it"))
      //test close task
      import com.supertaskhelper.domain.FeedbackJsonFormat._
      Post("/api/tasks/close",feedback) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
      }
      import com.supertaskhelper.domain.FeedbacksJsonFormat._
      Get("/api/users/feedbacks?userId="+userId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Feedbacks].feedbacks.size == 1)
        assert(responseAs[Feedbacks].feedbacks(0).rating ==5)
        assert(responseAs[Feedbacks].feedbacks(0).description =="feedback test")

      }

      import com.supertaskhelper.domain.TasksJsonFormat._
      Get("/api/tasks?id=" + taskId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Tasks].tasks(0).status == TASK_STATUS.CLOSED.toString)
      }
    }





    "accept request to delte a task" in {
      Delete("/api/tasks?id=" + taskId.get) ~> route ~> check {
        status should be(StatusCodes.OK)


      }



      import com.supertaskhelper.domain.TaskCategoriesJsonFormat._
      Get("/api/tasks/category") ~> route ~>check {
        status should be(StatusCodes.OK)
        assert(responseAs[TaskCategories].categories.size >0)
        assert(responseAs[TaskCategories].categories(0).categoryType.isDefined)
        assert(responseAs[TaskCategories].categories(0).description.isDefined)
        assert(responseAs[TaskCategories].categories(0).title_en.isDefined)
        assert(responseAs[TaskCategories].categories(0).title_it.isDefined)
        assert(!responseAs[TaskCategories].categories(0).id.isEmpty)
      }

      Get("/api/tasks/category?type=OFFLINE") ~> route ~>check {
        status should be(StatusCodes.OK)
        assert(responseAs[TaskCategories].categories.size >0)
        for(x <- responseAs[TaskCategories].categories)
          assert(x.categoryType.get == "OFFLINE")
      }
      Get("/api/tasks/category?type=ONLINE") ~> route ~>check {
        status should be(StatusCodes.OK)
        assert(responseAs[TaskCategories].categories.size >0)
        for(x <- responseAs[TaskCategories].categories)
          assert(x.categoryType.get == "ONLINE")
      }
      Get("/api/tasks/category?type=MT") ~> route ~>check {
        status should be(StatusCodes.OK)
        assert(responseAs[TaskCategories].categories.size >0)
        for(x <- responseAs[TaskCategories].categories)
          assert(x.categoryType.get == "MT")
      }
      Get("/api/tasks/category?type=MTsdsds") ~> route ~>check {
        assert(rejections.size ==1)
        assert(rejections(0).isInstanceOf[ValidationRejection])

      }


      Get("/api/tasks?id=" + taskId.get) ~> route ~> check {
        status should be(StatusCodes.NotFound)

      }
    }

  }
}
