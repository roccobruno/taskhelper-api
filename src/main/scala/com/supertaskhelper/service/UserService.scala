package com.supertaskhelper.service

import com.supertaskhelper.{ Settings, MongoFactory }
import com.supertaskhelper.domain.{ UserRegistration, User }
import com.mongodb.casbah.Imports._
import java.util.{ Locale, GregorianCalendar, Calendar, Date }
import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import com.mongodb.casbah.commons.conversions.MongoConversionHelper
import com.mongodb.casbah.commons.MongoDBObject

import com.supertaskhelper.common.domain.Password
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.common.enums.ACCOUNT_STATUS
import java.util
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:01
 * To change this template use File | Settings | File Templates.
 */

trait UserService extends Service {

  /**
   * finds a user by username
   * @param email
   * @return
   */
  def findUserByEmail(email: String) = {
    val q = MongoDBObject("email" -> email)
    val collection = MongoFactory.getCollection("user")
    getUser(collection, q)

  }

  private def getUser(collection: MongoCollection, query: MongoDBObject) = {
    val res = collection findOne query
    var user: User = null
    if (res != None) {

      val userResult = res.get

      user = User(
        userName = userResult.getAs[String]("username").get,
        isSTH = userResult.getAs[Boolean]("STH").get,
        email = userResult.getAs[String]("email").get,
        password = userResult.getAs[String]("password").get,
        id = userResult.getAs[ObjectId]("_id").get.toString
      )
      (true, user)
    } else
      (false, user)
  }

  def findUserById(id: String) = {

    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))
    val collection = MongoFactory.getCollection("user")
    getUser(collection, q)
  }

  def removeToken(email: String) {

    val collection = MongoFactory.getCollection("user_auth_token")
    collection remove (MongoDBObject("email" -> email))
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

    val duration = ConfigFactory.load().getInt("api-sth.token-session-duration")
    val tokenDB = getToken(token, email)

    if (tokenDB._1) {
      val time = new GregorianCalendar()
      val timeToken = new GregorianCalendar()
      timeToken.setTime(tokenDB._2.activeFrom)

      timeToken.add(Calendar.MINUTE, duration)

      if (timeToken after time) {
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

  def saveUser(registrationUser: UserRegistration, locale: Locale) {
    val collection = MongoFactory.getCollection("user")
    collection.save(MongoDBObject(
      "username" -> registrationUser.userName,
      "firstName" -> registrationUser.userName,
      "isSTH" -> false,
      "email" -> registrationUser.email,
      "password" -> Password.getSaltedHash(registrationUser.password),
      "accountStatus" -> ACCOUNT_STATUS.ACTIVE.toString,
      "withAccount" -> true,
      "accountCreatedDate" -> new Date(),
      "preferredLanguage" -> locale.toString
    ))

  }

  def updateTimeToken(token: UserToken) = {
    val collection = MongoFactory.getCollection("user_auth_token")
    val query = MongoDBObject("token" -> token.token, "email" -> token.username)

    collection update (query, $set("activeFrom" -> new Date()))
  }

  def getUserName(userId: String, email: String) {

  }

}

case class UserToken(username: String, token: String, activeFrom: Date)

