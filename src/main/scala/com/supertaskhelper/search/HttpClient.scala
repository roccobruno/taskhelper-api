package com.supertaskhelper.search

import akka.actor._
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import com.supertaskhelper.{ Settings, DefaultTimeout }

trait HttpClient extends Actor with ActorLogging with DefaultTimeout {

  val settings = Settings(context.system)

  implicit val ec = context.system.dispatcher

  val transport = IO(Http)(spray.util.actorSystem)
}
