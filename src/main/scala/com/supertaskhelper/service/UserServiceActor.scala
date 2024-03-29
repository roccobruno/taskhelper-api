package com.supertaskhelper.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.supertaskhelper.common.enums.SOURCE
import com.supertaskhelper.common.jms.alerts.ConfirmationEmailAlert
import com.supertaskhelper.domain.FeedbacksJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.TaskCategoriesJsonFormat._
import com.supertaskhelper.domain.UserJsonFormat._
import com.supertaskhelper.domain.search.UserSearchParams
import com.supertaskhelper.domain.{LocaleLanguage, Response, UserRegistration}
import com.supertaskhelper.service.UserServiceActor.{CreateUser, DeleteUser, FindFeedbacks, FindSkills}
import com.supertaskhelper.util.ActorFactory
import org.bson.types.ObjectId
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 11:23
 * To change this template use File | Settings | File Templates.
 */
class UserServiceActor(ctx: RequestContext) extends Actor with ActorLogging with ActorFactory
    with UserService with AlertMessageService with EmailSentService with CountryCheckerService {
  def receive = LoggingReceive {

    case CreateUser(user: UserRegistration, language: String, ipAddtess: Option[String]) => {

      log.info(s"ip:${ipAddtess}")

      if (UserUtil.isAlreadyUsed(user.email)) {
        ctx.complete(StatusCodes.BadRequest, CacheHeader(MaxAge404), "Email already used")
      } else {

        val id = saveUser(user, LocaleLanguage.getLocaleFromLanguage(language), checkCountryForIpAddress(ipAddtess))
        val alertActor = createSendAlertActor(context)
        alertActor ! new ConfirmationEmailAlert(user.email, generateEmailCode(user.email), language, SOURCE.valueOf(user.source.get))
        ctx.complete(Response("Resource Added", id))
      }

      context.stop(self)

    }

    case FindSkills(userId: String) => {
      if (!ObjectId.isValid(userId))
        ctx.complete(StatusCodes.BadRequest, CacheHeader(MaxAge404), "UserId not valid")
      else {
        val skills = findUserSkills(userId)
        ctx.complete(skills)
      }
      context.stop(self)

    }

    case FindFeedbacks(userId: String) => {
      if (!ObjectId.isValid(userId))
        ctx.complete(StatusCodes.BadRequest, CacheHeader(MaxAge404), "UserId not valid")
      else {
        val feedbacks = findUserFeebacks(userId)
        ctx.complete(feedbacks)
      }
      context.stop(self)

    }

    case s: UserSearchParams => {
      try {
        val res = if (!s.email.isEmpty) findUserByEmail(s.email.get) else findUserById(s.id.get)

        if (res._1)
          ctx.complete(res._2)
        else
          ctx.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
      } catch {
        case _ =>
          log.warning("User NOT FOUND with params:{}", s)
          ctx.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
      }
      context.stop(self)
    }

    case s: DeleteUser => {

      deleteUser(s.id)
      ctx.complete(Response("Success", s.id))
      context.stop(self)
    }

  }
}

object UserServiceActor {

  case class FindUser(id: String)
  case class DeleteUser(id: String) {
    require(!id.isEmpty, "id is mandatory")
    require(ObjectId.isValid(id), "id provided not valid")
  }
  case class CreateUser(user: UserRegistration, language: String, ipAddress: Option[String])
  case class FindSkills(userId: String)
  case class FindFeedbacks(userId: String)
  def props(name: String) = Props(classOf[UserServiceActor], name)

  def props() = Props(classOf[UserServiceActor])
}
