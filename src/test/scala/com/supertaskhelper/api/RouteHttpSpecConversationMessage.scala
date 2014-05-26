package com.supertaskhelper.api

import org.scalatest.{ Matchers, WordSpecLike }
import spray.testkit.ScalatestRouteTest
import concurrent.duration._
import com.supertaskhelper.router.RouteHttpService
import spray.http.StatusCodes
import com.supertaskhelper.domain._
import java.util.Locale

import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.Response
import com.supertaskhelper.domain.Location
import com.supertaskhelper.domain.Address
import spray.routing.AuthenticationFailedRejection
import com.supertaskhelper.common.enums.{ACCOUNT_STATUS, SOURCE}
import com.supertaskhelper.security.UserToken
import com.supertaskhelper.service.{ConversationMessageService, UserService}
import com.supertaskhelper.domain.MessagesJsonFormat._

import com.supertaskhelper.domain.ConversationJsonFormat._
import com.supertaskhelper.domain.ConversationsJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._



/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecConversationMessage extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)




  def actorRefFactory = system

  "The API Service" should {


    "should save the message" in new RouteHttpSpecConversationMessage with ConversationMessageService
      {
      val msg = Message(None,None,Option("Test message"),Option("test message"),Option("test_rocco_login@msn.com"),None,Option("test_rocco"),None,Option("test_pippo@msn.com"),None,None,None)
      var messageID_1 = ""
      var messageID_2 = ""
       Post("/api/conversation",msg) ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Response].message.contains("Success"))
         messageID_1 =  responseAs[Response].id
       }

      val messageSaved = findMessageById(messageID_1)


      val msg1 = Message(None,Option(messageSaved.get.conversationId.get),Option("Re:Test message"),Option("test test risposta"),Option("test_pippo@msn.com"),None,Option("test_rocco"),None,Option("test_rocco_login@msn.com"),None,None,None)

      Post("/api/conversation",msg1) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Success"))
        messageID_2 =  responseAs[Response].id
      }



      Get("/api/conversation?userId="+"5383766d036457876c3dc24f") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Conversations].convs.isEmpty == false)
        assert(responseAs[Conversations].convs.size == 1)
        assert(responseAs[Conversations].convs(0).id == messageSaved.get.conversationId.get)
        assert(responseAs[Conversations].convs(0).topic.get  == "Test message")
        assert(responseAs[Conversations].convs(0).users.get.contains("5383766d036457876c3dc24f"))
        assert(responseAs[Conversations].convs(0).users.get.contains("5383735e03641e76a78bded1"))
      }


      Get("/api/conversation?userId="+"5383735e03641e76a78bded1") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Conversations].convs.isEmpty == false)
        assert(responseAs[Conversations].convs.size == 1)
        assert(responseAs[Conversations].convs(0).id == messageSaved.get.conversationId.get)
        assert(responseAs[Conversations].convs(0).topic.get  == "Test message")
        assert(responseAs[Conversations].convs(0).users.get.contains("5383766d036457876c3dc24f"))
      }
      val mgsIds = Seq(messageID_1,messageID_2)
      Get("/api/conversation?id="+messageSaved.get.conversationId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Messages].messages.isEmpty == false)
        assert(responseAs[Messages].messages.size == 2)
        assert(mgsIds.contains(responseAs[Messages].messages(0).id.get))

      }


      Delete("/api/conversation?id="+messageSaved.get.conversationId.get+"&userId=5383766d036457876c3dc24f") ~> route ~>check {
        status should be(StatusCodes.OK)
      }
      //after deletion of a user I check that the conversation has been updated
      Get("/api/conversation?userId="+"5383735e03641e76a78bded1") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Conversations].convs.isEmpty == false)
        assert(responseAs[Conversations].convs.size == 1)
        assert(!responseAs[Conversations].convs(0).users.get.contains("5383766d036457876c3dc24f"))
        assert(responseAs[Conversations].convs(0).users.size == 1)
      }

      Get("/api/conversation?userId="+"5383766d036457876c3dc24f") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Conversations].convs.isEmpty == true)
      }



      mgsIds.foreach(x => deleteMessage(x))

      deleteConversation(messageSaved.get.conversationId.get)
    }
  }
}
