package com.supertaskhelper.service

import com.supertaskhelper.{ Settings, MongoFactory }
import com.supertaskhelper.domain.{ UserRegistration, User }
import com.mongodb.casbah.Imports._
import java.util.{ GregorianCalendar, Calendar, Date }
import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import com.mongodb.casbah.commons.conversions.MongoConversionHelper
import com.mongodb.casbah.commons.MongoDBObject

import com.supertaskhelper.common.domain.Password
import com.mongodb.casbah.commons.MongoDBObject

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:01
 * To change this template use File | Settings | File Templates.
 */

trait UserService {

  /**
   * finds a user by username
   * @param email
   * @return
   */
  def findUserByUserName(email: String) = {
    val q = MongoDBObject("email" -> email)
    val collection = MongoFactory.getCollection("user")
    var res = collection findOne q
    var user: User = null
    if (res != None) {

      val userResult = res.get

      user = User(
        userName = userResult.getAs[String]("username").get,
        isSTH = userResult.getAs[Boolean]("STH").get,
        email = userResult.getAs[String]("email").get,
        password = userResult.getAs[String]("password").get
      )
      (true, user)
    } else
      (false, user)

  }

  /**
   * creates a token, saves it into mondodb and return
   * @param userName
   */
  def createToken(userName: String): String = {
    val token = java.util.UUID.randomUUID.toString
    val collection = MongoFactory.getCollection("user_auth_token")
    collection remove (MongoDBObject("email" -> userName))

    collection save (MongoDBObject(
      "email" -> userName,
      "token" -> token,
      "activeFrom" -> new Date()

    ))
    token
  }

  def isValidToken(token: String, email: String): Boolean = {
    var valid = false

    val duration = 1 //TODO read from config
    val tokenDB = getToken(token, email)

    if (tokenDB._1) {
      val time = new GregorianCalendar()
      val timeToken = new GregorianCalendar()
      timeToken.setTime(tokenDB._2.activeFrom)

      timeToken.add(Calendar.MINUTE, duration)

      if (timeToken before time) {
        updateTimeToken(tokenDB._2)
        valid = true
      }

      valid
    } else
      false

  }

  private def getToken(token: String, username: String): (Boolean, UserToken) = {
    val collection = MongoFactory.getCollection("user_auth_token")
    val res = collection findOne (MongoDBObject("token" -> token, "email" -> username))
    var userToken: UserToken = null
    if (res != None) {
      val tokenRes = res.get
      userToken = UserToken(
        username = tokenRes.getAs[String]("email").get,
        token = tokenRes.getAs[String]("token").get,
        activeFrom = tokenRes.getAs[java.util.Date]("activeFrom").get

      )
      (true, userToken)
    } else
      (false, userToken)
  }

  def saveUser(registrationUser: UserRegistration) {
    val collection = MongoFactory.getCollection("user")
    collection.save(MongoDBObject(
      "username" -> registrationUser.userName,
      "isSTH" -> false,
      "email" -> registrationUser.email,
      "passwrod" -> Password.getSaltedHash(registrationUser.password)
    ))

  }

  def updateTimeToken(token: UserToken) = {
    val collection = MongoFactory.getCollection("user_auth_token")
    val query = MongoDBObject("token" -> token.token, "email" -> token.username)
    val update = MongoDBObject("activeFrom" -> new Date())
    collection update (query, update)
  }

}

case class UserToken(username: String, token: String, activeFrom: Date)

