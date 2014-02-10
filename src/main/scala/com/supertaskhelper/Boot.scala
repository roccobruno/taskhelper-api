package com.supertaskhelper

import spray.servlet.WebBoot
import akka.actor.{ Props, ActorSystem }
import com.netaporter.TopLevel
import com.supertaskhelper.router.RouteHttpServiceActor

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:36
 * To change this template use File | Settings | File Templates.
 */
class Boot extends WebBoot {

  val system = ActorSystem("api-sth")

  val serviceActor = system.actorOf(Props[RouteHttpServiceActor], "http-service")

}
