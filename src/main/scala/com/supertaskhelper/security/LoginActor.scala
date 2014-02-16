package com.supertaskhelper.security

import akka.actor.{ Actor, ActorLogging }
import spray.routing.RequestContext
import akka.event.LoggingReceive

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
class LoginActor(ctx: RequestContext) extends Actor with ActorLogging with UserAuthentication {
  def receive = LoggingReceive {

    case s: UserLogin => {

      //      ctx.complete(authenticateUserLogin(s.userName, s.password))
      //      context.stop(self)
    }

    case message @ _ =>
      log.warning(s"Unknown message received by LoginActor: ${message}")
  }
}
