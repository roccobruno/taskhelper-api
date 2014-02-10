package com.supertaskhelper.util

import akka.actor.{ ActorRef, Props, Actor }

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

}
