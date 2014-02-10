package com.netaporter

import akka.actor.{ Props, ActorSystem }
import spray.servlet.WebBoot

//object Boot extends App {
//
//  implicit val system = ActorSystem("digital-assets")
//  system.actorOf(TopLevel.props, TopLevel.name)
//
//}

class Boot extends WebBoot {

  // we need an ActorSystem to host our application in
  val system = ActorSystem("digital-assets")

  val serviceActor = system.actorOf(TopLevel.props, TopLevel.name)

  // the service actor replies to incoming HttpRequests
  //  val serviceActor = system.actorOf(Props[TopLevel])

}
