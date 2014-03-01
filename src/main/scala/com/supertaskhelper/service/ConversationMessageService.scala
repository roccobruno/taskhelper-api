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

import com.supertaskhelper.service.UserService
import com.supertaskhelper.common.enums.MESSAGE_STATUS

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 20/02/2014
 * Time: 19:57
 * To change this template use File | Settings | File Templates.
 */
trait ConversationMessageService extends UserService {

  def findConversation(params: ConversationParams): Seq[Conversation] = {

    var collection = MongoFactory.getCollection("conversation")
    var query = ("accessibleBy" $in Seq(params.userId.get))
    val res = (collection find query).skip((params.page.getOrElse(1) - 1) * params.pageSize.getOrElse(10))
      .limit(params.pageSize.getOrElse(10)).sort(MongoDBObject("lastUpdate" -> -1)).map(x => buildConversation(x)).toSeq

    res

  }

  private def buildConversation(convers: DBObject): Conversation = {

    Conversation(
      id = convers.getAs[String]("_id").get,
      lastUpdate = convers.getAs[Date]("lastUpdate"),
      users = Option(convers.get("accessibleBy").asInstanceOf[BasicDBList].map(x => x.toString).toSeq),
      topic = convers.getAs[String]("topic")

    )

  }

  def findConversationMessages(conversationParams: ConversationParams): Seq[Message] = {
    var collection = MongoFactory.getCollection("message")
    var query = MongoDBObject("conversationId" -> conversationParams.id.get)
    val res = (collection find query).skip((conversationParams.page.getOrElse(1) - 1) * conversationParams.pageSize.getOrElse(10))
      .limit(conversationParams.pageSize.getOrElse(10)).sort(MongoDBObject("dateSent" -> -1)).map(x => buildMessage(x)).toSeq

    res
  }

  private def buildMessage(message: DBObject): Message = {
    Message(
      id = Option(message.getAs[ObjectId]("_id").get.toString),
      conversationId = message.getAs[String]("conversationId"),
      subject = message.getAs[String]("subject"),
      message = message.getAs[String]("message"),
      fromEmail = message.getAs[String]("from"),
      fromUserId = message.getAs[String]("fromUserId"),
      createdDate = message.getAs[Date]("dateSent"),
      toEmail = message.getAs[String]("hiddenEmail"),
      toUserId = message.getAs[String]("toUserId"),
      status = message.getAs[String]("state"),
      toUserName = message.getAs[String]("to"),
      fromUserName = message.getAs[String]("fromName")

    )

  }

  def createMessage(message: CreateMessage): String = {
    val conversationId = message.message.conversationId.getOrElse(java.util.UUID.randomUUID().toString())
    //save message
    val collection = MongoFactory.getCollection("message")
    val dbmessage: MongoDBObject = buildDBBObjectMessage(message.message, conversationId)
    collection save dbmessage
    val collectionConv = MongoFactory.getCollection("conversation")
    if (message.message.conversationId.isDefined) {
      //update conversation obj

      val query = MongoDBObject("_id" -> conversationId)

      collectionConv update (query, $addToSet("accessibleBy" -> message.message.toUserId))
      collectionConv update (query, $set("lastUpdate" -> new Date()))
    } else {
      //craete conversation objectd
      collectionConv save buildDBObjectConversation(message.message, conversationId)
    }

    dbmessage.getAs[org.bson.types.ObjectId]("_id").get.toString
  }

  private def buildDBObjectConversation(message: Message, conversationId: String): MongoDBObject = {
    val obj = MongoDBObject(
      "_id" -> conversationId,
      "lastUpdate" -> new Date(),
      "topic" -> message.subject,
      "accessibleBy" -> Seq(message.toUserId)
    )
    obj
  }

  private def buildDBBObjectMessage(message: Message, conversationId: String): MongoDBObject = {
    MongoDBObject(
      "conversationId" -> conversationId,
      "fromName" -> (if (message.fromUserName.isDefined) message.fromUserName else getUserName(message.fromUserId.getOrElse(""),
        message.fromEmail.getOrElse(""))),
      "from" -> message.fromEmail,
      "fromUserId" -> message.fromUserId,
      "to" -> message.toUserName,
      "hiddenEmail" -> message.toEmail,
      "toUserId" -> message.toUserId,
      "message" -> message.message,
      "subject" -> message.subject,
      "state" -> MESSAGE_STATUS.UNREAD.toString,
      "dateSent" -> new Date(),
      "accessibleBy" -> Seq(message.fromUserId, message.toUserId)
    )
  }

}

