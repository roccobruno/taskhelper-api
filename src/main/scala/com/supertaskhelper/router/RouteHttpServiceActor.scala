package com.supertaskhelper.router

import akka.actor.{ Props, ActorRef, ActorLogging, Actor }
import spray.util.SprayActorLogging
import spray.routing._

import spray.http.MediaTypes
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.{ Task, Status }
import akka.event.LoggingReceive

import com.supertaskhelper.domain.StatusJsonFormat._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import com.supertaskhelper.security.UserLoginJsonFormat._
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.service.TaskServiceActor
import com.supertaskhelper.service.TaskServiceActor.{ DeleteTask, CreateTask, FindTask }
import com.supertaskhelper.search.SearchActor
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.security.{ LoginActor, UserLogin, UserAuthentication }

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

trait RouteHttpService extends HttpService with UserAuthentication {

  private var requestCount = 0

  def createPerTaskActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[TaskServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  //  def createLoginActor(ctx: RequestContext): ActorRef =
  //    actorRefFactory.actorOf(Props(classOf[LoginActor], ctx), "login-actor")

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
      } ~
        path("tasks") {
          get {
            respondWithMediaType(MediaTypes.`application/json`) {
              parameters('id.as[String]) { id =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! FindTask(id)
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
