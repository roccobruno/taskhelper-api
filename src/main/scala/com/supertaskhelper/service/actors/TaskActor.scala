package com.supertaskhelper.service.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import com.supertaskhelper.domain.Tasks
import com.supertaskhelper.service.TaskService
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.util.ActorFactory

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 17:40
 * To change this template use File | Settings | File Templates.
 */
class TaskActor extends Actor with ActorLogging with ActorFactory with TaskService {
  def receive = LoggingReceive {

    case params: FindTask => {

      val task = findTask(params.params)
      val res = (if (task.size > 0) Tasks(task.filter(_.isDefined).map(x => x.get)) else TaskNotFound(""))

      sender ! res
      context.stop(self)
    }

    case message @ _ =>
      log.warning(s"Unknown message received by TaskActor: ${message}")

  }

}

case class TaskNotFound(taskId: String)
