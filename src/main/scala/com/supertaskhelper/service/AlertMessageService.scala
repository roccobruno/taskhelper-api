package com.supertaskhelper.service

import akka.actor.{ ActorContext, Props, ActorRef }
import com.supertaskhelper.amqp.SendingAlertsActor

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
trait AlertMessageService {

  def createSendAlertActor(context: ActorContext): ActorRef = {
    context.actorOf(Props(classOf[SendingAlertsActor]))
  }

}
