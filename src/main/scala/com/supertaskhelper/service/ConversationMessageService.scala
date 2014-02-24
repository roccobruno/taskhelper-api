package com.supertaskhelper.service

import com.supertaskhelper.domain._
import com.supertaskhelper.MongoFactory
import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import spray.routing.RequestContext
import scala.concurrent.duration._

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import spray.routing.RequestContext
import java.util.Date
import com.supertaskhelper.domain.Conversation
import com.supertaskhelper.domain.ConversationParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.ConversationsJsonFormat._
import com.supertaskhelper.domain.MessagesJsonFormat._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 20/02/2014
 * Time: 19:57
 * To change this template use File | Settings | File Templates.
 */
trait ConversationMessageService {

  def findConversation(params: ConversationParams): Seq[Conversation] = {

    var collection = MongoFactory.getCollection("conversation")
    var query = ("accessibleBy" $in Seq(params.userId.get))
    val res = (collection find query).skip((params.page.getOrElse(1) - 1) * params.pageSize.getOrElse(10))
      .limit(params.pageSize.getOrElse(10)).map(x => buildConversation(x)).toSeq

    res

  }

  private def buildConversation(convers: DBObject): Conversation = {

    Conversation(
      id = convers.getAs[String]("_id").get,
      lastUpdate = convers.getAs[Date]("lastUpdate"),
      users = convers.getAs[List[String]]("users"),
      topic = convers.getAs[String]("topic")
    )

  }

  def findConversationMessages(conversationParams: ConversationParams): Seq[Message] = {
    var collection = MongoFactory.getCollection("message")
    var query = MongoDBObject("conversationId" -> conversationParams.id.get)
    val res = (collection find query).skip((conversationParams.page.getOrElse(1) - 1) * conversationParams.pageSize.getOrElse(10))
      .limit(conversationParams.pageSize.getOrElse(10)).map(x => buildMessage(x)).toSeq

    res
  }

  private def buildMessage(message: DBObject): Message = {
    Message(
      id = message.getAs[ObjectId]("_id").get.toString,
      conversationId = message.getAs[String]("conversationId"),
      subject = message.getAs[String]("subject"),
      message = message.getAs[String]("message"),
      fromEmail = message.getAs[String]("fromEmail"),
      fromUserId = message.getAs[String]("fromUser"),
      createdDate = message.getAs[Date]("createdDate")

    )

  }

}

