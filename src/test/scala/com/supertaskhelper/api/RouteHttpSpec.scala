package com.supertaskhelper.api

import org.scalatest.{Matchers, WordSpecLike}
import spray.testkit.ScalatestRouteTest
import concurrent.duration._
import com.supertaskhelper.router.RouteHttpService
import org.scalatest.Matchers._
import spray.http.StatusCodes
import com.supertaskhelper.domain.{Response, Address, Location, Task}
import java.util.Date
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import spray.httpx.SprayJsonSupport._




/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpec extends WordSpecLike with ScalatestRouteTest  with Matchers with RouteHttpService  {

  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)

  val location = Location("40.1508677","16.2848214")
  val address = Address(Option("via carlo levi API"),Option("Senise"),"Italia",Option(location),
    "85038",Option("Basilicata")
  )

  val task  = Task(None,"Api Task Test","Api Task test desc",new Date(),address,new Date(),"17.00","OPEN","53028f49036462126f7f042b",None,None,None,Option("Tuttofare"),
  Option("52515bb0e4b094388a43ca39")


  )

  def actorRefFactory = system

  "The API Service" should {

    var taskId:Option[String] = None

    "return the status message" in  new RouteHttpSpec  {
      Get("/api/status") ~> route ~> check {
        responseAs[String] should include ("\"status\": \"API-STH is running\"")
      }
    }

    "accept request to add task to index" in {
      Post("/api/tasks", task) ~> route ~> check {
        status should be (StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
        taskId = Option(responseAs[Response].id)
        println(taskId)
      }

      Delete("/api/tasks?id="+taskId.get) ~> route ~> check {
        status should be (StatusCodes.OK)

      }

    }


   }
}
