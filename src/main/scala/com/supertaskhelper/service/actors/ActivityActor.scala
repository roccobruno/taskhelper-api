package com.supertaskhelper.service.actors

import akka.actor.{ ActorLogging, Actor }
import com.supertaskhelper.util.ActorFactory
import com.supertaskhelper.service.ActivityService
import akka.event.LoggingReceive
import com.supertaskhelper.domain.search.ActivityParams
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.ActivitiesJsonFormat._
import com.supertaskhelper.domain.Activities

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 09/03/2014
 * Time: 19:51
 * To change this template use File | Settings | File Templates.
 */
class ActivityActor(httpContext: RequestContext) extends Actor with ActorLogging with ActorFactory with ActivityService {
  def receive = LoggingReceive {
    case params: ActivityParams => {
      httpContext.complete(Activities(findLastActivities(params)))
      context.stop(self)
    }

    case message @ _ => {
      log.warning(s"Unknown message received by ActivityActor: ${message}")
      context.stop(self)
    }
  }
}
