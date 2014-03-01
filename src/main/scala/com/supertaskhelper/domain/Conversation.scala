package com.supertaskhelper.domain

import java.util.Date
import spray.json._
import java.text.SimpleDateFormat
import com.supertaskhelper.domain.Conversations
import com.supertaskhelper.domain.Conversation
import com.supertaskhelper.domain.Messages
import com.supertaskhelper.domain.ConversationParams
import com.supertaskhelper.domain.Message

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 20/02/2014
 * Time: 19:52
 * To change this template use File | Settings | File Templates.
 */
case class Conversation(id: String, lastUpdate: Option[Date], users: Option[Seq[String]], topic: Option[String])
case class Message(id: Option[String], conversationId: Option[String], subject: Option[String], message: Option[String],
    fromEmail: Option[String], fromUserId: Option[String], fromUserName: Option[String], createdDate: Option[Date], toEmail: Option[String],
    toUserId: Option[String], toUserName: Option[String], status: Option[String]) {

  require(!message.isEmpty, "message cannot be empty")
  require(!subject.isEmpty, "subject cannot be empty")
  require(fromEmail.isDefined || fromUserId.isDefined, "either email or userId of the sender must be sent")
  require(toEmail.isDefined || toUserId.isDefined, "either email or userId of the receiver must be sent")
}

object ConversationJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val conversationFormat = jsonFormat4(Conversation)
}

object MessageJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val messageFormat = jsonFormat12(Message)
}

case class ConversationParams(id: Option[String], page: Option[Int], pageSize: Option[Int], userId: Option[String]) {

  require((id.isDefined || userId.isDefined), "either id or user Id must be supplied")
}

object ConversationParamsFormat extends DefaultJsonProtocol {
  implicit val conversationParamsFormat = jsonFormat4(ConversationParams)
}

case class Conversations(convs: Seq[Conversation])
object ConversationsJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.ConversationJsonFormat._
  implicit val conversationFormat = jsonFormat4(Conversation)
  implicit val conversationsFormat = jsonFormat1(Conversations)
}

case class Messages(messages: Seq[Message])
object MessagesJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.MessageJsonFormat._
  implicit val messageFormat = jsonFormat12(Message)
  implicit val messagesFormat = jsonFormat1(Messages)
}