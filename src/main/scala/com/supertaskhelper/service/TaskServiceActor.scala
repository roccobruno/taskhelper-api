package com.supertaskhelper.service

import akka.actor._
import com.supertaskhelper.util.ActorFactory
import akka.event.LoggingReceive
import com.supertaskhelper.service.TaskServiceActor._
import com.supertaskhelper.domain._
import scala.concurrent.duration._
import spray.routing.RequestContext
import com.supertaskhelper.domain.TaskJsonFormat._
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.ResponseJsonFormat._
import spray.routing.RequestContext
import com.supertaskhelper.amqp.SendingAlertsActor
import com.supertaskhelper.common.jms.alerts.{ CommentAddedAlert, BidAlert, BetterBidCreatedAlert, CreatedTaskAlert }
import spray.http.StatusCodes
import com.supertaskhelper.domain.TasksJsonFormat._
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import spray.routing.RequestContext
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.service.TaskServiceActor.CreateBid
import spray.routing.RequestContext
import com.supertaskhelper.service.TaskServiceActor.FindBids
import com.supertaskhelper.domain.TaskParams
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Tasks
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.domain.BidJsonFormat._
import com.supertaskhelper.domain.BidsJsonFormat._
import com.supertaskhelper.domain.CommentJsonFormat._
import com.supertaskhelper.domain.CommentsJsonFormat._
import com.supertaskhelper.domain.TaskCategoryJsonFormat._
import com.supertaskhelper.domain.TaskCategoriesJsonFormat._
import java.util.Date
import com.supertaskhelper.service.actors.{ TaskActor, TaskNotFound }

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:49
 * To change this template use File | Settings | File Templates.
 */
class TaskServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
    with TaskService with AlertMessageService {

  def createTaskActor(): ActorRef = {
    context.actorOf(Props(classOf[TaskActor]))
  }

  val timeout = Duration(context.system.settings.config.getMilliseconds("api-sth.per-request-actor.timeout"), MILLISECONDS)
  context.setReceiveTimeout(timeout)
  import com.supertaskhelper.domain.TaskJsonFormat._
  import com.supertaskhelper.domain.ResponseJsonFormat._
  def receive = LoggingReceive {

    case t: Task => {
      httpRequestContext.complete(t)
      context.stop(self)
    }

    case t: Tasks => {
      t.tasks.foreach(x => println(x))

      httpRequestContext.complete(t)
      context.stop(self)
    }

    case ts: TaskNotFound => {
      httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
      context.stop(self)
    }

    case f: FindTask => {
      log.info("Received request to search task with params:{}", f)
      val actor = createTaskActor()
      actor ! f
    }

    case FindBids(taskId: String) => {

      val task = findTaskById(taskId)
      if (task.isDefined) httpRequestContext.complete(Bids(task.get.bids.get.sortWith(_.createdDate after _.createdDate)))
      else
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")

      context.stop(self)
    }

    case FindComments(taskId: String) => {
      val task = findTaskById(taskId)
      if (task.isDefined) httpRequestContext.complete(Comments(task.get.comments.get.sortWith(_.dateCreated after _.dateCreated)))
      else
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      context.stop(self)
    }

    case DeleteTask(id: String) =>
      log.info("Received request to delete task with id:{}", id)
      deleteTask(id.toString)
      httpRequestContext.complete(Response("Success",id))
      context.stop(self)
    case CreateTask(task: Task, language: String) =>
      log.info("Received request to create the  task :{}", task)
      val response = createTask(task)
      val sendAlertActor = createSendAlertActor(context)
      sendAlertActor ! new CreatedTaskAlert(response.id, language)
      httpRequestContext.complete(response)
      context.stop(self)

    case CreateBid(bid: Bid, language: String) => {
      log.info("Received request to create the  bid :{}", bid)

      val task = findTaskById(bid.taskId.get)
      if (!task.isDefined)
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      val betterBid = task.get.bids.get.filter(b => b.incrementedValue < bid.incrementedValue)
      val bidId = "BID_" + (new Date()).getTime
      val response = createBid(bid, bidId)
      val sendAlertActor = createSendAlertActor(context)
      if (betterBid.size > 0) {
        //send notification normal bid
        sendAlertActor ! new BidAlert(bid.taskId.get, bidId, language);
      } else {
        //send best bid notification
        sendAlertActor ! new BetterBidCreatedAlert(bid.taskId.get, language);
      }
      httpRequestContext.complete(response)
      context.stop(self)
    }

    case CreateComment(comment: Comment, language: String) => {
      log.info("Received request to create the  Comment :{}", comment)
      val task = findTaskById(comment.taskId)
      if (!task.isDefined)
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      val commentId = comment.taskId + "-" + findTaskById(comment.taskId).get.comments.size
      val response = createComment(comment, commentId)
      val sendAlertActor = createSendAlertActor(context)
      sendAlertActor ! new CommentAddedAlert(comment.taskId, commentId, comment.userId, language, false);
      httpRequestContext.complete(response)
      context.stop(self)

    }

    case f: FindTaskCategory => {
      httpRequestContext.complete(findTaskCategory(f.categoryType))
      context.stop(self)
    }
    case ReceiveTimeout =>
      context.stop(self)
    case message @ _ =>
      log.warning(s"Unknown message received by TaskServiceActor: ${message}")

  }
}

object TaskServiceActor {

  case class FindTask(params: TaskParams)
  case class DeleteTask(id: String)
  case class CreateTask(task: Task, language: String)
  case class FindBids(taskId: String)
  case class FindComments(taskId: String)
  case class CreateBid(bid: Bid, language: String)
  case class CreateComment(comment: Comment, language: String)
  case class FindTaskCategory(categoryType: Option[String])

  def props(name: String) = Props(classOf[TaskServiceActor], name)

  def props() = Props(classOf[TaskServiceActor])
}
