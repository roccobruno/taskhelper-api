package com.supertaskhelper.service

import java.util.{Calendar, Date, GregorianCalendar, Locale}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.TypeImports.{BasicDBObject, ObjectId}
import com.supertaskhelper.MongoFactory
import com.supertaskhelper.common.domain.Password
import com.supertaskhelper.common.enums.{ACCOUNT_STATUS, TASK_TYPE}
import com.supertaskhelper.domain.{Address, Location, User, UserRegistration, _}
import com.supertaskhelper.util.ConverterUtil
import com.typesafe.config.ConfigFactory
import org.bson.types.ObjectId

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:01
 * To change this template use File | Settings | File Templates.
 */

trait UserService extends Service with ConverterUtil {


  def buildDBFeedbackObg(feedback: Feedback): MongoDBObject = {
    MongoDBObject(
    "userId" -> feedback.userId,
    "created_date" -> feedback.createdDate,
    "description" -> feedback.description,
    "rating" -> feedback.rating,
    "taskId" -> feedback.taskId
    )

  }

  def updateUserWithRatingAndAddFeedback(f:Feedback) = {

    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(f.sthId.get))

    val user = findUserById(f.userId)._2

    val numTot: Int = user.numOfFeedbacks.getOrElse(0) + 1
    val newAverage: Int = ((user.averageRating.getOrElse(0) * (numTot - 1)) + f.rating) / numTot
    collection update(q,$set("averageRating" -> newAverage,"numOfFeedbacks" -> numTot))
    collection update(q,$push(("feedbacks", buildDBFeedbackObg(f))))
  }

  def findUserSkills(userId: String): TaskCategories = {
    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(userId))
    val fields = MongoDBObject("onlineSkills" -> 1, "offlineSkills" -> 1)
    var res = collection findOne (q, fields)

    var skills: Seq[TaskCategory] = Seq()

    if (res.isDefined) {
      val userResult = res.get
      if (userResult.get("onlineSkills").asInstanceOf[BasicDBList] != null) {
        skills = skills ++ userResult.get("onlineSkills").asInstanceOf[BasicDBList].map(x => buildTaskCategory(x.asInstanceOf[BasicDBObject], TASK_TYPE.ONLINE.toString)).toSeq
      }
      if (userResult.get("offlineSkills").asInstanceOf[BasicDBList] != null) {
        skills = skills ++ userResult.get("offlineSkills").asInstanceOf[BasicDBList].map(x => buildTaskCategory(x.asInstanceOf[BasicDBObject], TASK_TYPE.OFFLINE.toString)).toSeq
      }

      TaskCategories(skills)

    } else {
      TaskCategories(Seq())
    }

  }

  def findUserFeebacks(userId: String): Feedbacks = {
    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(userId))
    val fields = MongoDBObject("feedbacks" -> 1)
    var res = collection findOne (q, fields)

    var skills: Seq[Feedback] = Seq()

    if (res.isDefined) {
      val userResult = res.get
      if (userResult.get("feedbacks").asInstanceOf[BasicDBList] != null) {
        skills = skills ++ (userResult.get("feedbacks").asInstanceOf[BasicDBList].map(x => buildFeedback(x.asInstanceOf[BasicDBObject]))).toSeq.sortWith(_.createdDate after _.createdDate)
      }

      Feedbacks(skills)

    } else {
      Feedbacks(Seq())
    }

  }

  private def buildFeedback(feedback: DBObject): Feedback = {
    Feedback(
      userId = feedback.getAs[String]("userId").getOrElse(""),
      description = feedback.getAs[String]("description").getOrElse(""),
      createdDate = feedback.getAs[Date]("created_date").getOrElse(new Date()),
      taskId = feedback.getAs[String]("taskId").getOrElse(""),
      rating = feedback.getAs[Int]("rating").getOrElse(1),
      sthId = Some(""),
      language = Some("")

    )
  }

  private def buildTaskCategory(category: DBObject, tasktype: String): TaskCategory = {
    TaskCategory(
      id = category.getAs[ObjectId]("_id").get.toString,
      categoryType = Option(tasktype),
      title_it = category.getAs[String]("title_it"),
      title_en = category.getAs[String]("title_en"),
      description = category.getAs[String]("description"),
      order = category.getAs[Int]("order"),
      tariff = category.getAs[String]("tariff")
    )
  }

  /**
   * finds a user by username
   * @param email
   * @return
   */
  def findUserByEmail(email: String): (Boolean, User) = {
    val q = MongoDBObject("email" -> email)
    val collection = MongoFactory.getCollection("user")
    getUser(collection, q)

  }

  def activateAccount(email: String) {
    val update = MongoDBObject(
      "$set" -> MongoDBObject("accountStatus" -> ACCOUNT_STATUS.ACTIVE.toString)
    )
    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("email" -> email)
    collection update (q, update)
  }

  def deleteUser(id: String) = {
    val collection = MongoFactory.getCollection("user")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))
    collection remove q
  }

  private def getUser(collection: MongoCollection, query: MongoDBObject) = {
    val res = collection findOne query
    var user: User = null
    if (res != None) {

      val userResult = res.get
      val addobj = userResult.getAsOrElse[BasicDBObject]("address", null)
      val locationObj = if (addobj != null) addobj.getAsOrElse[BasicDBObject]("location", null) else null
      val location: Option[Location] = if (locationObj != null) { Option(Location(locationObj.getString("longitude"), locationObj.getString("latitude"))) } else None

      val address: Option[Address] = if (addobj != null) Option(Address(Option(addobj.getString("address")), Option(addobj.getString("city")), addobj.getString("country"), location,
        Option(addobj.getString("postcode")), Option(addobj.getString("regione"))))
      else None

      user = User(
        userName = userResult.getAs[String]("username").get,
        lastName = userResult.getAs[String]("lastName").get,

        isSTH = userResult.getAs[Boolean]("STH").getOrElse(false),
        email = userResult.getAs[String]("email").get,
        password = userResult.getAs[String]("password").get,
        id = userResult.getAs[ObjectId]("_id").get.toString,
        imgUrl = Option("loadphoto/USER_" + userResult.getAs[ObjectId]("_id").get.toString),
        distance = None,
        address = address,
        bio = userResult.getAs[String]("bio"),
        fbBudget = userResult.getAs[Boolean]("fbBudget"),
        twitterBudget = userResult.getAs[Boolean]("twitterBudget"),
        linkedInBudget = userResult.getAs[Boolean]("linkedInBudget"),
        securityDocVerified = userResult.getAs[Boolean]("securityDocVerified"),
        webcamVerified = userResult.getAs[Boolean]("webcamVerified"),
        emailVerified = userResult.getAs[Boolean]("emailVerified"),
        idDocVerified = userResult.getAs[Boolean]("idDocVerified"),
        accountStatus = userResult.getAs[String]("accountStatus"),
        averageRating = userResult.getAs[Int]("averageRating"),
        numOfFeedbacks =  userResult.getAs[Int]("numOfFeedbacks")

      )
      (true, user)
    } else
      (false, user)
  }

  def findUserById(id: String): (Boolean, User) = {
    if (!ObjectId.isValid(id))
      (false, null)
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

  def saveUser(registrationUser: UserRegistration, locale: Locale): String = {
    val collection = MongoFactory.getCollection("user")
    //first have to delete previous requests
    val q = MongoDBObject("email" -> registrationUser.email)
    collection remove q

    val userDoc = getUserMongoDBObject(registrationUser, locale)
    collection.save(userDoc)
    userDoc.getAs[org.bson.types.ObjectId]("_id").get.toString
  }

  def getUserMongoDBObject(registrationUser: UserRegistration, locale: Locale): MongoDBObject = {
    if (registrationUser.address.isDefined) {
      MongoDBObject(
        "username" -> registrationUser.userName,
        "firstName" -> registrationUser.userName,
        "lastName" -> registrationUser.lastname,
        "isSTH" -> false,
        "email" -> registrationUser.email,
        "password" -> Password.getSaltedHash(registrationUser.password),
        "accountStatus" -> ACCOUNT_STATUS.TOAPPROVE.toString,
        "withAccount" -> true,
        "accountCreatedDate" -> new Date(),
        "preferredLanguage" -> locale.toString,
        "address" -> getMongoDBObjFromAddress(registrationUser.address.get))
    } else {
      MongoDBObject(
        "username" -> registrationUser.userName,
        "firstName" -> registrationUser.userName,
        "lastName" -> registrationUser.lastname,
        "isSTH" -> false,
        "email" -> registrationUser.email,
        "password" -> Password.getSaltedHash(registrationUser.password),
        "accountStatus" -> ACCOUNT_STATUS.TOAPPROVE.toString,
        "withAccount" -> true,
        "accountCreatedDate" -> new Date(),
        "preferredLanguage" -> locale.toString)
    }
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

