package com.supertaskhelper.service

import spray.routing.RequestContext
import akka.actor.{ ActorLogging, Actor }
import scala.concurrent.duration._
import spray.routing.RequestContext
import akka.event.LoggingReceive
import com.supertaskhelper.domain._
import com.supertaskhelper.domain.MessagesJsonFormat._
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.Conversations
import com.supertaskhelper.domain.Messages
import com.supertaskhelper.domain.ConversationParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.Message
import com.supertaskhelper.service.CreateMessage
import com.supertaskhelper.common.jms.alerts.EmailAlert
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.ResponseJsonFormat._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 24/02/2014
 * Time: 20:32
 * To change this template use File | Settings | File Templates.
 */
class ConversationMessageActor(ctx: RequestContext) extends Actor with ActorLogging with ConversationMessageService with AlertMessageService {
  val timeout = Duration(context.system.settings.config.getMilliseconds("api-sth.per-request-actor.timeout"), MILLISECONDS)
  context.setReceiveTimeout(timeout)
  import com.supertaskhelper.domain.ConversationsJsonFormat._

  def receive = LoggingReceive {

    case c: ConversationParams => {

      c.id match {
        case None => {
          ctx.complete(Conversations(findConversation(c)))
        }
        case _ => {
          ctx.complete(Messages(findConversationMessages(c)))
        }
      }

      context.stop(self)

    }

    case c: CreateMessage => {
      val messageId = createMessage(c)
      //send alert email
      val sendAlertActor = createSendAlertActor(context)
      sendAlertActor ! new EmailAlert(messageId, c.language)
      //complete
      ctx.complete(Response("Success", messageId))
      context.stop(self)
    }

    case c: DeleteConversation => {
      deleteUserFromConversation(c.id, c.userId)
      ctx.complete(Response("Success", "1"))
      context.stop(self)
    }

    case message @ _ =>
      log.warning(s"Unknown message received by ConversationMessageActor: ${message}")

  }

}

case class CreateMessage(message: Message, language: String)
case class DeleteConversation(id: String, userId: String)