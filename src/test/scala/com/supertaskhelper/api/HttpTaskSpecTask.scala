//package com.supertaskhelper.api
//
//import org.scalatest.{Matchers, WordSpecLike}
//import spray.testkit.ScalatestRouteTest
//import com.supertaskhelper.router.RouteHttpService
//import akka.actor.ActorRefFactory
//import spray.http.StatusCodes
//import com.supertaskhelper.domain.Tasks
//import com.supertaskhelper.domain.TaskJsonFormat._
//
//import com.supertaskhelper.domain.ResponseJsonFormat._
//import spray.httpx.SprayJsonSupport._
//import com.supertaskhelper.domain.Response
//import com.supertaskhelper.domain.Location
//import com.supertaskhelper.domain.Task
//import com.supertaskhelper.domain.Address
//import com.supertaskhelper.domain.TaskJsonFormat._
//import com.supertaskhelper.domain.TasksJsonFormat._
//import com.supertaskhelper.domain.CommentAnswerJsonFormat._
//
///**
// * Created with IntelliJ IDEA.
// * User: r.bruno@london.net-a-porter.com
// * Date: 15/06/2014
// * Time: 15:37
// * To change this template use File | Settings | File Templates.
// */
//class HttpTaskSpecTask extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {
//
//
//  "The API Service" should {
//
//    var taskId: Option[String] = None
//
//    "return the status message" in new RouteHttpSpecTask {
//      Get("/api/tasks?id=525c616de4b08b71eeafa59d") ~> route ~> check {
//        status should be(StatusCodes.OK)
//        assert(responseAs[Tasks].tasks(0).id == taskId)
//      }
////    }
//  }
//
//  implicit def actorRefFactory: ActorRefFactory = system
//}
