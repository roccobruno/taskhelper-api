package com.supertaskhelper.service

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import com.supertaskhelper.domain.AccountJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.{Account, Response}
import com.supertaskhelper.service.AccountServiceActor.FindAccount
import com.supertaskhelper.util.ActorFactory
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.RequestContext

class AccountServiceActor(httpRequestContext: RequestContext) extends Actor with ActorFactory with ActorLogging
    with UserService with AccountService {
  def receive = LoggingReceive {

    case f: FindAccount => {
      httpRequestContext.complete(findAccount(f.userId))
      context.stop(self)
    }
    case account: Account => {
      saveAccount(account)
      httpRequestContext.complete(Response("Resource Added", "1"))
      context.stop(self)
    }

  }
}

object AccountServiceActor {

  case class FindAccount(userId: String) {
    require(!userId.isEmpty, "userId cannot be empty")
  }

  object FindAccountJsonFormat extends DefaultJsonProtocol {

    implicit val findAccountFormat = jsonFormat1(FindAccount)
  }
}
