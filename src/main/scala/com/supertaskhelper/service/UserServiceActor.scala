package com.supertaskhelper.service

import akka.actor.{ Props, ActorLogging, Actor }
import com.supertaskhelper.util.ActorFactory
import akka.event.LoggingReceive

import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.common.domain.Password
import com.supertaskhelper.domain.search.UserSearchParams
import spray.routing.RequestContext

import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.domain.search.UserSearchParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.ResponseJsonFormat._
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.UserJsonFormat._
import com.supertaskhelper.domain.{ Response, LocaleLanguage, UserRegistration }
import com.supertaskhelper.common.jms.alerts.ConfirmationEmailAlert
import com.supertaskhelper.common.enums.SOURCE
import spray.http.StatusCodes

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 11:23
 * To change this template use File | Settings | File Templates.
 */
class UserServiceActor(ctx: RequestContext) extends Actor with ActorLogging with ActorFactory
    with UserService with AlertMessageService with EmailSentService {
  def receive = LoggingReceive {

    case CreateUser(user: UserRegistration, language: String) => {

      saveUser(user, LocaleLanguage.getLocaleFromLanguage(language))
      val alertActor = createSendAlertActor(context)
      alertActor ! new ConfirmationEmailAlert(user.email, generateEmailCode(user.email), language, SOURCE.valueOf(user.source.get))
      ctx.complete(Response("Resource Added", 200.toString))
      context.stop(self)

    }

    case s: UserSearchParams => {
      val res = if (!s.email.isEmpty) findUserByEmail(s.email.get) else findUserById(s.id.get)
      if (res._1)
        ctx.complete(res._2)
      else
        ctx.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
      context.stop(self)
    }

  }
}

object UserServiceActor {

  case class FindUser(id: String)
  case class DeleteUser(id: Int)
  case class CreateUser(user: UserRegistration, language: String)

  def props(name: String) = Props(classOf[UserServiceActor], name)

  def props() = Props(classOf[UserServiceActor])
}
