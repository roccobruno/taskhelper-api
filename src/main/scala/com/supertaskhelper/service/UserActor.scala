package com.supertaskhelper.service

import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import com.supertaskhelper.domain.User
import com.mongodb.casbah.Imports._
import com.supertaskhelper.service.UserNotFound
import com.supertaskhelper.domain.User
import com.supertaskhelper.service.FindUser

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 06/03/2014
 * Time: 22:25
 * To change this template use File | Settings | File Templates.
 */
class UserActor extends Actor with ActorLogging with UserService {
  def receive = LoggingReceive {

    case f: FindUser => {

      val res = findUserById(f.id)
      if (res._1)
        sender ! copyUser(res._2, f.distance)
      else
        sender ! UserNotFound(f.id)

    }
  }

  private def copyUser(u: User, dist: Option[String]): User = {
    User(
      userName = u.userName,
      isSTH = u.isSTH,
      email = u.email,
      password = u.password,
      id = u.id,
      imgUrl = u.imgUrl,
      distance = dist,
      address = u.address,
      bio = u.bio,
      fbBudget = u.fbBudget,
      twitterBudget = u.twitterBudget,
      linkedInBudget = u.linkedInBudget,
      securityDocVerified = u.securityDocVerified,
      webcamVerified = u.webcamVerified,
      emailVerified = u.emailVerified,
      idDocVerified = u.idDocVerified

    )

  }
}

case class FindUser(id: String, distance: Option[String])
case class UserNotFound(taskId: String)
