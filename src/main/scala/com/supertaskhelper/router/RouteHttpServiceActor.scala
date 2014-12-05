package com.supertaskhelper.router

import akka.actor._
import akka.event.LoggingReceive
import com.supertaskhelper.domain.AccountJsonFormat._
import com.supertaskhelper.domain.CommentAnswerJsonFormat._
import com.supertaskhelper.domain.FeedbackJsonFormat._
import com.supertaskhelper.domain.MessageJsonFormat._
import com.supertaskhelper.domain.PaymentJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.StatusJsonFormat._
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.domain.UpdateTaskStatusParamsFormat._
import com.supertaskhelper.domain.UserRegistrationJsonFormat._
import com.supertaskhelper.domain.search.{ActivityParams, SearchParams, UserSearchParams}
import com.supertaskhelper.domain.{Bid, Comment, ConversationParams, Message, Payment, Response, Status, Task, TaskParams, UserRegistration, _}
import com.supertaskhelper.search.SearchActor
import com.supertaskhelper.security.UserAuthentication
import com.supertaskhelper.security.UserTokensonFormat._
import com.supertaskhelper.service.AccountServiceActor.FindAccount
import com.supertaskhelper.service.DashboardServiceActor.LoadDashboard
import com.supertaskhelper.service.PaymentServiceActor.CapturePaymentFormat._
import com.supertaskhelper.service.PaymentServiceActor.TransferPaymentFormat._
import com.supertaskhelper.service.PaymentServiceActor.{CapturePayment, DeletePayment, FindPayment, TransferPayment}
import com.supertaskhelper.service.TaskServiceActor.{CreateBid, CreateComment, CreateTask, DeleteTask, FindBids, FindComments, FindTask, FindTaskCategory, _}
import com.supertaskhelper.service.UserServiceActor.{CreateUser, _}
import com.supertaskhelper.service.actors.ActivityActor
import com.supertaskhelper.service.{Code, CreateMessage, EmailSentService, PaymentServiceActor, TaskServiceActor, _}
import spray.http.MediaTypes
import spray.httpx.SprayJsonSupport._
import spray.routing.{RequestContext, _}

import scala.concurrent.ExecutionContext.Implicits.global

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

  def createPerPaymentActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[PaymentServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  def createPerDashboardActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[DashboardServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  def createPerAccountActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[AccountServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  def createPerTaskActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[TaskServiceActor], ctx), s"IndexRequest-${requestCount}")
  }

  def createEmailSentActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[EmailSentActor], ctx), s"email-code-actor-${requestCount}")
  }

  def createConversationMessageActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[ConversationMessageActor], ctx), s"conversation-message-code-actor-${requestCount}")
  }
  def createActivityActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[ActivityActor], ctx), s"activity-code-actor-${requestCount}")
  }

  def createPerUserActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[UserServiceActor], ctx), s"user-actor-${requestCount}")

  }
  def createSearchActor(ctx: RequestContext): ActorRef = {
    requestCount += 1
    actorRefFactory.actorOf(Props(classOf[SearchActor], ctx), s"search-actor-${requestCount}")
  }
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
      } ~ path("users" / "skills") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters('userId.as[String]) { userId =>
              ctx =>
                val perRequestUserActor = createPerUserActor(ctx)
                perRequestUserActor ! FindSkills(userId)
            }

          }
        }
      } ~ path("users" / "account" / RestPath) { id =>
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            ctx =>
              val paymentActor = createPerAccountActor(ctx)
              paymentActor ! FindAccount(id.toString())
          }
        }
      } ~ path("users" / "account") {
        post {
          respondWithMediaType(MediaTypes.`application/json`) {
            entity(as[Account]) { account =>
              ctx => val paymentActor = createPerAccountActor(ctx)
              paymentActor ! account

            }

          }
        }
      } ~ path("users" / "feedbacks") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters('userId.as[String]) { userId =>
              ctx =>
                val perRequestUserActor = createPerUserActor(ctx)
                perRequestUserActor ! FindFeedbacks(userId)
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
          } ~
          delete {
            respondWithMediaType(MediaTypes.`application/json`) {
              authenticate(authenticateUser) { user =>
                parameters('id.as[String]) { id =>
                  ctx =>
                    val perRequestSearchingActor = createPerUserActor(ctx)
                    perRequestSearchingActor ! DeleteUser(id)
                }
              }
            }
          }
      } ~ path("search") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            //            authenticate(authenticateUser) { user =>
            parameters('terms.as[String].?,
              'otype.as[String].?,
              'page.as[Int].?,
              'sizePage.as[Int].?,
              'sort.as[String].?,
              'position.as[String].?).as(SearchParams) { terms =>
                ctx =>
                  val perRequestSearchingActor = createSearchActor(ctx)
                  perRequestSearchingActor ! terms
                //              }
              }
          }
        }
      }~ path("tasks" / "toassign") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'taskId.as[String]) { taskId =>
              ctx =>
                val perRequestSearchingActor = createPerTaskActor(ctx)
                perRequestSearchingActor ! FindBids(taskId)
            }
          }
        }
      } ~ path("tasks" / "bids") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'taskId.as[String]) { taskId =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! FindBids(taskId)
              }
          }
        } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[Bid]) { bid =>
                ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
                perRequestSearchingActor ! CreateBid(bid, "it")

              }
            }
          }
      } ~ path("tasks" / "category") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'type.as[String].?).as(FindTaskCategory) { findTaskCategory =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! findTaskCategory
              }
          }
        }
      } ~ path("tasks" / "comments" / "answers") {
        delete {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'commentId.as[String]) { commentId =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! DeleteCommentAnswers(commentId)
              }
          }
        } ~
          get {
            respondWithMediaType(MediaTypes.`application/json`) {
              parameters(
                'commentId.as[String]) { commentId =>
                  ctx =>
                    val perRequestSearchingActor = createPerTaskActor(ctx)
                    perRequestSearchingActor ! FindCommentAnswers(commentId)
                }
            }
          } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[CommentAnswer]) { comment =>
                ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
                perRequestSearchingActor ! CreateCommentAnswer(comment, "it")

              }
            }
          }
      } ~ path("tasks" / "comments") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'taskId.as[String]) { taskId =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! FindComments(taskId)
              }
          }
        } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[Comment]) { comment =>
                ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
                perRequestSearchingActor ! CreateComment(comment, "it")

              }
            }
          }
      } ~ path("tasks" / "close") {
        post {
          respondWithMediaType(MediaTypes.`application/json`) {
            entity(as[Feedback]) { feedback =>
              ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
              perRequestSearchingActor ! feedback

            }
          }
        }
      } ~ path("tasks" / "update") {
        post {
          respondWithMediaType(MediaTypes.`application/json`) {
            entity(as[UpdateTaskStatusParams]) { params =>
              ctx => val perRequestSearchingActor = createPerTaskActor(ctx)
              perRequestSearchingActor ! UpdateTask(params)

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
              'sizePage.as[Int].?,
              'distance.as[String].?,
              'language.as[String].?,
              'bidSthId.as[String].?,
              'hireSthId.as[String].?).as(TaskParams) { params =>
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
              parameters('id.as[String]) { id =>
                ctx =>
                  val perRequestSearchingActor = createPerTaskActor(ctx)
                  perRequestSearchingActor ! DeleteTask(id)
              }
            }
          }
      } ~ path("conversation") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'id.as[String].?,
              'page.as[Int].?,
              'sizePage.as[Int].?,
              'userId.as[String].?).as(ConversationParams) { params =>
                ctx =>
                  val perRequestSearchingActor = createConversationMessageActor(ctx)
                  perRequestSearchingActor ! params
              }
          }

        } ~
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[Message]) { message =>
                ctx => val perRequestSearchingActor = createConversationMessageActor(ctx)
                perRequestSearchingActor ! CreateMessage(message, "it")

              }
            }
          } ~ delete {
            respondWithMediaType(MediaTypes.`application/json`) {
              parameters('id.as[String],
                'userId.as[String].?).as(DeleteConversation) { dltC =>
                  ctx => val perRequestSearchingActor = createConversationMessageActor(ctx)
                  perRequestSearchingActor ! dltC
                }
            }
          }
      } ~ path("activities") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'subjectId.as[String].?,
              'page.as[Int].?,
              'sizePage.as[Int].?,
              'positionId.as[Int].?).as(ActivityParams) { params =>
                ctx =>
                  val perRequestSearchingActor = createActivityActor(ctx)
                  perRequestSearchingActor ! params
              }
          }
        }
      } ~
        path("payments") {
          post {
            respondWithMediaType(MediaTypes.`application/json`) {
              entity(as[Payment]) { payment =>
                ctx => val paymentActor = createPerPaymentActor(ctx)
                paymentActor ! payment

              }

            }
          }
        } ~ path("payments" / "capture") {
          post {
            respondWithMediaType(MediaTypes.`application/json`) {

              entity(as[CapturePayment]) { payment =>
                ctx => val paymentActor = createPerPaymentActor(ctx)
                paymentActor ! payment

              }
            }
          }

        } ~ path("payments" / "transfer") {
          post {
            respondWithMediaType(MediaTypes.`application/json`) {

              entity(as[TransferPayment]) { payment =>
                ctx => val paymentActor = createPerPaymentActor(ctx)
                paymentActor ! payment

              }
            }
          }

        } ~ path("payments" / RestPath) { id =>
          get {
            respondWithMediaType(MediaTypes.`application/json`) {
              ctx =>
                val paymentActor = createPerPaymentActor(ctx)
                paymentActor ! FindPayment(id.toString())
            }
          } ~ delete {
            respondWithMediaType(MediaTypes.`application/json`) {
              ctx =>
                val paymentActor = createPerPaymentActor(ctx)
                paymentActor ! DeletePayment(id.toString())
            }
          }
        } ~ path("dashboard") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            parameters(
              'userId.as[String]).as(LoadDashboard)  { ldash =>
              ctx => val ldashActor = createPerDashboardActor(ctx)
                ldashActor ! ldash

            }
          }
        }
      }

    } ~
      path("") {
        getFromResource("web/index.html")
      }

}
