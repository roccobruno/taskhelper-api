package com.supertaskhelper.util

import akka.actor._
import akka.actor.SupervisorStrategy.{ Restart, Stop }

/**
 * Makes Actors
 */
trait ActorFactory {
  this: Actor =>

  def makeActor(props: Props): ActorRef = {
    context.actorOf(props)
  }

  def makeActor(props: Props, name: String): ActorRef = {
    context.actorOf(props, name)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => Restart
  }

}
