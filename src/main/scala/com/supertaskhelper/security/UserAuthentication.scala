package com.supertaskhelper.security

import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import spray.routing.AuthenticationFailedRejection
import spray.routing.authentication.{ Authentication, ContextAuthenticator }

import scala.concurrent.Future
import spray.json.DefaultJsonProtocol
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http.{ HttpChallenge, HttpHeaders }
import com.supertaskhelper.service.UserService
import com.supertaskhelper.common.domain.Password

case class UserToken(userName: String, token: String) {}

object UserJsonFormat extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat2(UserToken)
}

case class UserLogin(userName: String, password: String)

object UserLoginJsonFormat extends DefaultJsonProtocol {
  implicit val userLoginFormat = jsonFormat2(UserToken)
}

trait UserAuthentication extends UserService {

  val conf = ConfigFactory.load()
  lazy val configusername = conf.getString("security.username")
  lazy val configpassword = conf.getString("security.password")

  def authenticateUser: ContextAuthenticator[UserToken] = {
    ctx =>
      {
        //get username and password from the url
        val usr = ctx.request.uri.query.get("email").getOrElse("null")
        val pwd = ctx.request.uri.query.get("token").getOrElse("null")

        doAuth(usr, pwd)
      }
  }

  def authLogin: ContextAuthenticator[UserToken] = {
    ctx =>
      {
        val usr = ctx.request.uri.query.get("email").getOrElse("null")
        val pwd = ctx.request.uri.query.get("password").getOrElse("null")

        Future {

          val user = findUserByEmail(usr)

          Either.cond(user._1 && usr == user._2.email && Password.check(pwd, user._2.password),
            UserToken(userName = usr, token = createToken(user._2.email)),
            AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List(HttpHeaders.`WWW-Authenticate`(HttpChallenge("", "")))))
        }
      }
  }

  private def doAuth(userName: String, token: String): Future[Authentication[UserToken]] = {
    Future {
      Either.cond(isValidToken(token, userName),
        UserToken(userName = userName, token = java.util.UUID.randomUUID.toString),
        AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List(HttpHeaders.`WWW-Authenticate`(HttpChallenge("", "")))))
    }
  }
}