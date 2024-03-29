package com.supertaskhelper.api

import java.util.Date

import akka.actor.ActorRefFactory
import com.supertaskhelper.common.enums.{TASK_STATUS, TASK_TYPE}
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.{Response, Task, TaskPrice, _}
import com.supertaskhelper.router.RouteHttpService
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.testkit.ScalatestRouteTest

/**
* Created with IntelliJ IDEA.
* User: r.bruno@london.net-a-porter.com
* Date: 15/06/2014
* Time: 15:37
* To change this template use File | Settings | File Templates.
*/
class HttpTaskSpecTask extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {


  "The API Service" should {

    val taskPrice = TaskPrice(Option(true),Option("10"),Option(true),Option(2),Option(true),Option(3),Option("2"),Option("3"),Some(2))
    val taskBadge = TaskBadges(Option(true), Option(true), Option(true), Option(true), Option(true), Option(true), Option(true))
    var taskOnlineId: Option[String] = None

    val taskOnline = Task(None, "Api Task Test", "Api Task test desc", new Date(), None, new Date(),
      "17.00", TASK_STATUS.TOAPPROVEREQUEST.toString, "53028f49036462126f7f042b", None, None, None, Option("Tuttofare"),
      Option("52515bb0e4b094388a43ca39"), TASK_TYPE.ONLINE.toString,Option(taskBadge),"WITH_AUCTION_ONLY",Option("52515bb0e4b094388a43ca39"),Option(taskPrice),Option(true),None,
    Some("EUR")

    )

    var taskId: Option[String] = None
     import com.supertaskhelper.domain.TaskJsonFormat._
    "return the status message" in new RouteHttpSpecTask {
      //post task online
      Post("/api/tasks", taskOnline) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
        taskOnlineId = Option(responseAs[Response].id)

      }

      Get("/api/tasks?id=" + taskOnlineId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      Delete("/api/tasks?id=" + taskOnlineId.get) ~> route ~> check {
        status should be(StatusCodes.OK)


      }

    }
  }

  implicit def actorRefFactory: ActorRefFactory = system
}
