package com.netaporter

import akka.actor._
import spray.can.Http
import akka.io.IO
import akka.actor.Terminated
import akka.actor.SupervisorStrategy.{ Restart, Stop }
import akka.util.Timeout
import scala.reflect.io.File

object TopLevel {
  def props: Props = Props[ProductionTopLevel]
  def name = "top-level"
}

class ProductionTopLevel extends TopLevel with TopLevelConfig {
  private def c = context.system.settings.config
  def interface = c.getString("example-app.service.interface")
  def port = c.getInt("example-app.service.port")
  def rootDir = c.getString("root.dir.asserts")

  //  def portbees = File(System.getenv("PWD") + "/.genapp/ports").toDirectory.list.next().name.toInt
  //  log.info("port+" + portbees)
  log.info("J:" + System.getenv("sun.java.command"))
  log.info("J2:" + System.getenv("PWD"))
  log.info("J3:" + File(System.getenv("PWD") + "/.genapp/ports"))
  log.info("J3:" + File(System.getenv("PWD") + "/.genapp/ports").isDirectory)

  // This timeout is just for resource cleanup.
  // Make sure it is 10% longer than spray's request timeout.
  implicit def askTimeout = Timeout(c.getMilliseconds("spray.can.client.request-timeout") * 11 / 10)

  def createModel = context.actorOf(AssetActor.props, AssetActor.name)
  def createService(model: ActorRef) = context.actorOf(ServiceActor.props(model), ServiceActor.name)
}

trait TopLevelConfig {
  def createModel: ActorRef
  def createService(model: ActorRef): ActorRef
  def interface: String
  def port: Int
  //  def portbees: Int
}

trait TopLevel extends Actor with ActorLogging {
  this: TopLevelConfig =>

  val model = createModel
  context watch model

  val service = createService(model)
  context watch service

  import context._
  //  IO(Http) ! Http.Bind(service, interface, port)

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ if model == sender => Stop
    case _ if service == sender => Restart
  }

  def receive = {
    case Http.CommandFailed(_) => context stop self
    case Terminated(`model`) => context stop self
    case _ => service
  }

}
