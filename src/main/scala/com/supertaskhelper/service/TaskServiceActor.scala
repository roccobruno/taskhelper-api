package com.supertaskhelper.service

import java.util.Date

import akka.actor._
import akka.event.LoggingReceive
import com.supertaskhelper.common.enums.{TASK_REQUEST_TYPE, TASK_STATUS}
import com.supertaskhelper.common.jms.alerts._
import com.supertaskhelper.domain.BidsJsonFormat._
import com.supertaskhelper.domain.CommentsJsonFormat._
import com.supertaskhelper.domain.TaskCategoriesJsonFormat._
import com.supertaskhelper.domain.TasksJsonFormat._
import com.supertaskhelper.domain.{Bid, Bids, Comment, CommentAnswer, Comments, Response, Task, TaskParams, Tasks, _}
import com.supertaskhelper.service.TaskServiceActor.{CreateBid, CreateComment, CreateCommentAnswer, CreateTask, DeleteCommentAnswers, DeleteTask, FindBids, FindCommentAnswers, FindComments, FindTask, FindTaskCategory, _}
import com.supertaskhelper.service.actors.{TaskActor, TaskNotFound}
import com.supertaskhelper.util.ActorFactory
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:49
 * To change this template use File | Settings | File Templates.
 */
class TaskServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
    with TaskService with UserService with AlertMessageService {

  def createTaskActor(): ActorRef = {
    context.actorOf(Props(classOf[TaskActor]))
  }

  val timeout = Duration(context.system.settings.config.getMilliseconds("api-sth.per-request-actor.timeout"), MILLISECONDS)
  context.setReceiveTimeout(timeout)
  import com.supertaskhelper.domain.ResponseJsonFormat._
  import com.supertaskhelper.domain.TaskJsonFormat._
  def receive = LoggingReceive {

    case f: Feedback => {
      //update task status
      updateTaskStatus(f.taskId, TASK_STATUS.CLOSED.toString)
      val sendAlertActor = createSendAlertActor(context)
      sendAlertActor ! new ClosedTaskAlert(f.taskId, f.language.getOrElse("it"))

      updateUserWithRatingAndAddFeedback(f)

      httpRequestContext.complete(Response("Success", f.taskId))
      context.stop(self)
    }

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
      if (task.isDefined) {
        val bids: Bids = if (task.get.bids.isDefined) Bids(task.get.bids.get.sortWith(_.createdDate.get after _.createdDate.get))
        else Bids(Seq())
        httpRequestContext.complete(bids)

      } else
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")

      context.stop(self)
    }

    case FindComments(taskId: String) => {
      val task = findTaskById(taskId)
      if (task.isDefined) {
        val comments: Comments = if (task.get.comments.isDefined) Comments(task.get.comments.get.sortWith(_.dateCreated after _.dateCreated))
        else Comments(Seq())
        httpRequestContext.complete(comments)
      } else
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      context.stop(self)
    }

    case FindCommentAnswers(commentId: String) => {
      val res = findCommentAnswerByCommentId(commentId)
      if (res.isDefined) {
        httpRequestContext.complete(res.get)
      } else
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Comments Not Found")
      context.stop(self)
    }

    case DeleteCommentAnswers(commentId: String) => {
      log.info("Received request to delete comment answer  with commentId:{}", commentId)
      deleteCommentAnswers(commentId)
      httpRequestContext.complete(Response("Success", commentId))
      context.stop(self)
    }

    case DeleteTask(id: String) =>

      log.info("Received request to delete task with id:{}", id)
      deleteTask(id.toString)
      httpRequestContext.complete(Response("Success", id))
      context.stop(self)
    case CreateTask(task: Task, language: String) => {
      log.info("Received request to create the  task :{}", task)
      val response = createTask(task, language)

      val sendAlertActor = createSendAlertActor(context)

      if (task.hireSthId.isDefined) {
        //task request. need to send ale(rt only to the choosen STH
        sendAlertActor ! new TaskRequestAlert(response.id, language)

      } else {
        //normal task open to bid
        sendAlertActor ! new CreatedTaskAlert(response.id, language)
      }

      httpRequestContext.complete(response)
      context.stop(self)
    }

    case UpdateTask(params: UpdateTaskStatusParams) => {
      log.info("Received request to update the  taskid :{}", params.id)
      val sendAlertActor = createSendAlertActor(context)
      //update task status
      val status = params.status

      if (params.status == TASK_STATUS.REQUESTACCEPTED.toString) {
        //task request. need to send ale(rt only to the choosen STH

        createBidToTask(params.id)

        updateTaskStatus(params.id, params.status)
        sendAlertActor ! new AcceptedTaskRequestAlert(params.id, params.language.getOrElse("it"))
      }
      if (params.status == TASK_STATUS.REQUESTREFUSED.toString) {
        //task request. need to send ale(rt only to the choosen STH
        updateTaskStatus(params.id, params.status)
        sendAlertActor ! new RejectedTaskRequestAlert(params.id, params.language.getOrElse("it"))
      }
      if (params.status == TASK_STATUS.TOVERIFY.toString) {
        //task request. need to send ale(rt only to the choosen STH
        val task: Task = findTask(TaskParams(Some(params.id), None, None, None, None, None, None, None, None, None, None, None))(0).get
        val newRequetType: TASK_REQUEST_TYPE = if (task.requestType.equalsIgnoreCase(TASK_REQUEST_TYPE.WITH_DIRECT_HIRE.toString))
          TASK_REQUEST_TYPE.WITH_AUCTION_FROM_DIRECT_HIRE else TASK_REQUEST_TYPE.WITH_AUCTION_FROM_DIRECT_HIRE_WITH_TARIFF

        updateTaskStatusAndRequestType(params.id, params.status, newRequetType.toString)

        sendAlertActor ! new CreatedTaskAlert(params.id, params.language.getOrElse("it"))
      }

      if (params.status == TASK_STATUS.COMPLETED.toString) {
        //task request. need to send ale(rt only to the choosen STH
        updateTaskStatus(params.id, params.status)
        sendAlertActor ! new CompletedTaskAlert(params.id, params.language.getOrElse("it"))

      }

      httpRequestContext.complete(Response("Success", params.id))
      context.stop(self)

    }

    case CreateBid(bid: Bid, language: String) => {
      log.info("Received request to create the  bid :{}", bid)

      val task = findTaskById(bid.taskId.get)
      if (!task.isDefined)
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      else {
        val betterBid = task.get.bids.getOrElse(Seq()).filter(b => b.incrementedValue < bid.incrementedValue)
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
      }
      context.stop(self)
    }

    case CreateComment(comment: Comment, language: String) => {
      log.info("Received request to create the  Comment :{}", comment)
      val task = findTaskById(comment.taskId)
      if (!task.isDefined) {
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      } else {
        val commentId = comment.taskId + "-" + findTaskById(comment.taskId).get.comments.size
        val response = createComment(comment, commentId)
        val sendAlertActor = createSendAlertActor(context)
        sendAlertActor ! new CommentAddedAlert(comment.taskId, commentId, comment.userId, language, false);
        httpRequestContext.complete(response)
      }

      context.stop(self)

    }

    case CreateCommentAnswer(comment: CommentAnswer, language: String) => {
      log.info("Received request to create the  CommentAnswer :{}", comment)
      val task = findTaskById(comment.taskId)
      if (!task.isDefined) {
        httpRequestContext.complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Task Not Found")
      } else {

        val response = createCommentAnswer(comment)
        val sendAlertActor = createSendAlertActor(context)
        sendAlertActor ! new CommentAddedAlert(comment.taskId, comment.commentId.get, comment.userId, language, true);
        httpRequestContext.complete(response)
      }

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
  case class UpdateTask(params: UpdateTaskStatusParams)
  case class DeleteTask(id: String)
  case class CreateTask(task: Task, language: String)
  case class FindBids(taskId: String)
  case class FindComments(taskId: String)
  case class FindCommentAnswers(commentId: String)
  case class DeleteCommentAnswers(commentId: String)
  case class CreateBid(bid: Bid, language: String)
  case class CreateComment(comment: Comment, language: String)
  case class CreateCommentAnswer(comment: CommentAnswer, language: String)
  case class FindTaskCategory(categoryType: Option[String]) {
    require(!categoryType.isDefined
      || categoryType.get == "ONLINE" || categoryType.get == "OFFLINE" || categoryType.get == "MT", "wrong value passed for type. The accepted ones are:ONLINE,OFFLINE and MT")
  }

  def props(name: String) = Props(classOf[TaskServiceActor], name)

  def props() = Props(classOf[TaskServiceActor])
}
