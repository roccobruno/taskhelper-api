package com.supertaskhelper.service

import akka.actor._
import com.supertaskhelper.util.ActorFactory
import akka.event.LoggingReceive
import com.supertaskhelper.service.TaskServiceActor.{ DeleteTask, CreateTask, FindTask }
import com.supertaskhelper.domain.{ Tasks, TaskParams, Task, Response }
import scala.concurrent.duration._
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import spray.routing.RequestContext
import com.supertaskhelper.domain.TaskJsonFormat._
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.service.TaskServiceActor.DeleteTask
import spray.routing.RequestContext
import com.supertaskhelper.service.TaskServiceActor.CreateTask
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.amqp.SendingAlertsActor
import com.supertaskhelper.common.jms.alerts.CreatedTaskAlert
import spray.http.StatusCodes
import com.supertaskhelper.domain.TasksJsonFormat._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:49
 * To change this template use File | Settings | File Templates.
 */
class TaskServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging with TaskService with AlertMessageService {

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

    case FindTask(params: TaskParams) => {
      log.info("Received request to search task with params:{}", params)
      val actor = createTaskActor()
      actor ! params
    }

    case DeleteTask(id: Int) =>
      log.info("Received request to delete task with id:{}", id)
      context.stop(self)
    case CreateTask(task: Task, language: String) =>
      log.info("Received request to create the  task :{}", task)
      val response = createTask(task)
      val sendAlertActor = createSendAlertActor(context)
      sendAlertActor ! new CreatedTaskAlert(response.id, language)
      httpRequestContext.complete(response)
      context.stop(self)
    case ReceiveTimeout =>
      context.stop(self)
    case message @ _ =>
      log.warning(s"Unknown message received by TaskServiceActor: ${message}")

  }
}

object TaskServiceActor {

  case class FindTask(params: TaskParams)
  case class DeleteTask(id: Int)
  case class CreateTask(task: Task, language: String)

  def props(name: String) = Props(classOf[TaskServiceActor], name)

  def props() = Props(classOf[TaskServiceActor])
}
