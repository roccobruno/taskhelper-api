package com.supertaskhelper.service

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import com.supertaskhelper.domain.DashboardJsonFormat._
import com.supertaskhelper.service.DashboardServiceActor.LoadDashboard
import com.supertaskhelper.util.ActorFactory
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.RequestContext

class DashboardServiceActor (httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
with DashboardService{

  def receive = LoggingReceive {

    case load: LoadDashboard => {
      httpRequestContext.complete(loadDashboardData(Some(load.userId)))
      context.stop(self)
    }

  }

}

object DashboardServiceActor {
  case class LoadDashboard(userId:String) {
    require(!userId.isEmpty,"userId cannot be empty")
  }

  object LoadDashboardJsonFormat extends DefaultJsonProtocol {
    implicit val loadDashboardFormat = jsonFormat1(LoadDashboard)
  }
}
