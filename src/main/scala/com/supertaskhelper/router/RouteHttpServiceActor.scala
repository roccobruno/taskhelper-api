package com.supertaskhelper.router

import akka.actor._
import spray.util.{ LoggingContext, SprayActorLogging }
import spray.routing._

import spray.http.{ StatusCodes, MediaTypes }
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain._
import akka.event.LoggingReceive

import com.supertaskhelper.domain.StatusJsonFormat._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import com.supertaskhelper.security.UserLoginJsonFormat._
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.service._
import com.supertaskhelper.service.TaskServiceActor.{ DeleteTask, CreateTask, FindTask }
import com.supertaskhelper.search.SearchActor
import com.supertaskhelper.domain.search.{ UserSearchParams, SearchParams }
import com.supertaskhelper.security.{ Logout, LoginActor, UserLogin, UserAuthentication }
import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.domain.search.UserSearchParamsJsonFormat._
import com.supertaskhelper.domain.UserRegistrationJsonFormat._
import com.supertaskhelper.domain.search.UserSearchParams
import com.supertaskhelper.domain.search.UserSearchParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.domain.search.UserSearchParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.domain.search.UserSearchParams
import spray.routing.RequestContext
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.service.Code
import com.supertaskhelper.service.UserServiceActor.CreateUser
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.domain.UserRegistration
import com.supertaskhelper.domain.Status
import com.supertaskhelper.domain.Task
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.TaskParamsFormat._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:37
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpServiceActor extends Actor with RouteHttpService with ActorLogging {

  def actorRefFactory = context

  def receive = LoggingReceive {
    runRoute(route)
  }
}

trait RouteHttpService extends HttpService with UserAuthentication with EmailSentService {

  private var requestCount = 0

  def createPerTaskActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[TaskServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  def createEmailSentActor(ctx: RequestContext): ActorRef = {
    actorRefFactory.actorOf(Props(classOf[EmailSentActor], ctx), "email-code-actor")
  }

  def createPerUserActor(ctx: RequestContext): ActorRef =
    actorRefFactory.actorOf(Props(classOf[UserServiceActor], ctx), "user-actor")

  def createSearchActor(ctx: RequestContext): ActorRef = actorRefFactory.actorOf(Props(classOf[SearchActor], ctx), "search-actor")

  val route: Route =
    pathPrefix("api") {
      path("status") {
        complete(Status("API-STH is running"))
      } ~ path("login") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            authenticate(authLogin) { user =>
              ctx => {
                ctx.complete(user)
              }

            }
          }
        }
      } ~ path("logout") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters('email.as[String]) { email =>
              ctx => {
                logout(email)
                ctx.complete(Response("the user has been logged out", 200.toString))
              }

            }
          }
        }
      } ~ path("verifycode") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters('codeEmail.as[String]) { code =>
              ctx =>
                //verify code
                val emailActor = createEmailSentActor(ctx)
                emailActor ! Code(code)
            }
          }
        }
      } ~ path("users") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters('id.as[String].?, 'email.as[String].?).as(UserSearchParams) { userSearchParams =>
              ctx =>
                val perRequestUserActor = createPerUserActor(ctx)
                perRequestUserActor ! userSearchParams
            }

          }
        } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[UserRegistration]) { user =>
                ctx => val perRequestSearchingActor = createPerUserActor(ctx)
                perRequestSearchingActor ! CreateUser(user, "it")

              }
            }
          }
      } ~ path("search") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            authenticate(authenticateUser) { user =>
              parameters('terms.as[String]) { terms =>
                ctx =>
                  val perRequestSearchingActor = createSearchActor(ctx)
                  perRequestSearchingActor ! SearchParams(terms, "task")
              }
            }
          }
        }
      } ~ path("tasks") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'id.as[String].?,
              'status.as[String].?,
              'tpId.as[String].?,
              'sthId.as[String].?,
              'sort.as[String].?,
              'city.as[String].?,
              'page.as[Int].?,
              'sizePage.as[Int].?).as(TaskParams) { params =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! FindTask(params)
              }
          }
        } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[Task]) { task =>
                ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
                perRequestSearchingActor ! CreateTask(task, "it")

              }
            }
          } ~
          delete {
            respondWithMediaType(MediaTypes.`application/json`) {
              parameters('id.as[Int]) { id =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! DeleteTask(id)
              }
            }
          }
      }
    } ~
      path("") {
        getFromResource("web/index.html")
      }

}
