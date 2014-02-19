package com.supertaskhelper.service

import akka.actor.{ ActorLogging, Actor }
import akka.event.LoggingReceive
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.util.ActorFactory
import com.supertaskhelper.domain.{ Tasks, TaskParams }

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 17:40
 * To change this template use File | Settings | File Templates.
 */
class TaskActor extends Actor with ActorLogging with ActorFactory with TaskService {
  def receive = LoggingReceive {

    case params: TaskParams => {

      val task = findTask(params)
      sender ! (if (task.size > 0) Tasks(task) else TaskNotFound(""))
    }

    case message @ _ =>
      log.warning(s"Unknown message received by TaskActor: ${message}")

  }
}

case class TaskNotFound(taskId: String)
