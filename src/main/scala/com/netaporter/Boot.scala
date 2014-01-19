package com.netaporter

import akka.actor.ActorSystem

object Boot extends App {

  implicit val system = ActorSystem("digital-assets")
  system.actorOf(TopLevel.props, TopLevel.name)

}
