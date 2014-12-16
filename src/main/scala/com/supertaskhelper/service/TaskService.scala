package com.supertaskhelper.service

import java.util.Date

import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.MongoFactory
import com.supertaskhelper.common.enums.{COMMENT_STATUS, TASK_REQUEST_TYPE, TASK_STATUS, TASK_TYPE}
import com.supertaskhelper.common.util.PaymentUtil
import com.supertaskhelper.domain.{Address, Location, Response, Task, _}
import com.supertaskhelper.util.ConverterUtil
import org.bson.types.ObjectId;

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 23:23
 * To change this template use File | Settings | File Templates.
 */
trait TaskService extends Service with ConverterUtil with CityService with MongodbService with UserService {

  def createBidToTask(taskId: String) = {

    val task = findTaskById(taskId).get;

    val valueFromSuggestedPrice = PaymentUtil.getValueWithoutFee(new java.math.BigDecimal(task.taskPrice.get.priceSuggested.getOrElse("0")))
    val offeredValue = if (task.taskPrice.get.tariffWithoutFeeForSth.isDefined) BigDecimal(task.taskPrice.get.tariffWithoutFeeForSth.get).*(BigDecimal(task.taskPrice.get.numOfWorkingHour.get))
    else BigDecimal(valueFromSuggestedPrice.toString)
    val sthRes = findUserById(task.hireSthId.get)
    val sthUsername = if (sthRes._1) sthRes._2.userName else ""
    val bid = Bid(Option(new Date()),
      offeredValue.toString(),
      BigDecimal(task.taskPrice.get.priceSuggested.get).toString(),
      task.hireSthId.get, sthUsername, "", Some(task.id.get.toString), Some("BID_" + (new Date()).getTime), None)

    createBid(bid, bid.id.get)
  }

  def assignTask(taskId: String, sthId: String) = {
    val task = findTaskById(taskId);
    if (task.isDefined) {

      updateTaskAttribute(taskId, $set("status" -> TASK_STATUS.ASSIGNED.toString,
        "taskHelperId" -> sthId,
        "assignedDate" -> new Date(),
        "withHire" -> false,
        "hireSthId" -> sthId))

    }

  }
  def findTask(params: TaskParams): Seq[Option[Task]] = {

    val q = buildQueryDsl(params)
    //    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(params.id.get))
    val collection = MongoFactory.getCollection("task")
    var result = (collection find q).sort(MongoDBObject("createdDate" -> -1))
      .skip((params.page.getOrElse(1) - 1) * params.sizePage.getOrElse(10))
      .limit(params.sizePage.getOrElse(10)).map(x => buildTask(x, params.distance)).toSeq
    result
  }

  private def buildQueryDsl(params: TaskParams): DBObject = {
    var res: List[(String, Any)] = List()
    if (params.id.isDefined && ObjectId.isValid(params.id.get))
      res = res ::: List("_id" -> new org.bson.types.ObjectId(params.id.get))

    if (params.city.isDefined)
      res = res ::: List("address.city" -> params.city.get)

    if (params.status.isDefined) {

      if (params.status.get.startsWith("in")) {
        val wrapped = """(?<=\().+?(?=\))""".r;
        val values = wrapped.findFirstIn(params.status.get).get.split(",")
        res = res ::: List("status" -> MongoDBObject("$in" -> values))

      } else
        res = res ::: List("status" -> params.status.get)
    }
    if (params.tpId.isDefined)
      res = res ::: List("userId" -> params.tpId.get)

    if (params.sthId.isDefined)
      res = res ::: List("taskHelperId" -> params.sthId.get)

    if (params.hireSthId.isDefined)
      res = res ::: List("hireSthId" -> params.hireSthId.get)

    if (params.bidSthId.isDefined)
      res = res ::: List("bids.taskhelperId" -> params.bidSthId.get)

    MongoDBObject(res)

  }

  private def buildQuery(params: TaskParams): DBObject = {
    val builder = MongoDBObject.newBuilder
    if (params.id.isDefined && ObjectId.isValid(params.id.get))
      builder += "_id" -> new org.bson.types.ObjectId(params.id.get)

    if (params.city.isDefined)
      builder += "address.city" -> params.city.get

    if (params.status.isDefined) {

      if (params.status.get.startsWith("in")) {
        val wrapped = """(?<=\().+?(?=\))""".r;
        val values = wrapped.findFirstIn(params.status.get).get.split(",")
        builder += "status" -> ("$in" -> values)

      } else
        builder += "status" -> params.status.get
    }
    if (params.tpId.isDefined)
      builder += "userId" -> params.tpId.get

    if (params.sthId.isDefined)
      builder += "taskHelperId" -> params.sthId.get

    if (params.hireSthId.isDefined)
      builder += "hireSthId" -> params.hireSthId.get

    if (params.bidSthId.isDefined)
      builder += "bids.taskhelperId" -> params.bidSthId.get

    builder.result

  }

  private def buildTask(taskResult: DBObject, distance: Option[String]): Option[Task] = {

    try {

      println(taskResult.getAs[ObjectId]("_id"))

      val bidss: Option[Seq[Bid]] =
        if (taskResult.get("bids").asInstanceOf[BasicDBList] != null) {
          Option(taskResult.get("bids").asInstanceOf[BasicDBList].map(x => buildBid(x.asInstanceOf[BasicDBObject], taskResult.getAs[ObjectId]("_id").get.toString)).toSeq.sortWith(_.createdDate.get after _.createdDate.get)
          )
        } else None

      val comms: Option[Seq[Comment]] = if (taskResult.get("comments").asInstanceOf[BasicDBList] != null) {
        Option(taskResult.get("comments").asInstanceOf[BasicDBList].map(x => buildComment(x.asInstanceOf[BasicDBObject], taskResult.getAs[ObjectId]("_id").get.toString)).toSeq.sortWith(_.dateCreated before _.dateCreated)
        )
      } else None
      val addobj = taskResult.get("address").asInstanceOf[BasicDBObject]
      val locationObj = if (addobj != null) addobj.get("location").asInstanceOf[BasicDBObject] else null
      val location: Option[Location] = if (locationObj != null) { Option(Location(locationObj.getString("longitude"), locationObj.getString("latitude"))) } else None

      val address = Address(Option(addobj.getString("address")), Option(addobj.getString("city")), addobj.getString("country"), location, Option(addobj.getString("postcode")), Option(addobj.getString("regione")))

      val badg = TaskBadges(taskResult.getAs[Boolean]("emailVerBudgetRequired"),
        taskResult.getAs[Boolean]("linkedInBudgetRequired"), taskResult.getAs[Boolean]("fbBudgetRequired"),
        taskResult.getAs[Boolean]("passportIdBudgetRequired"), taskResult.getAs[Boolean]("twitterBudgetRequired"), taskResult.getAs[Boolean]("secDocBudgetRequired"), taskResult.getAs[Boolean]("webcamBudgetRequired"))

      val priceObj = taskResult.get("price").asInstanceOf[BasicDBObject]
      val tariffWithoutFeeForSTH: Option[String] = if (priceObj != null) Option(priceObj.getAs[String]("tariffWithoutFeeForSTH").getOrElse("")) else None;
      val tariffWithFeeForSTH: Option[String] = if (priceObj != null) Option(priceObj.getAs[String]("tariffWithFeeForSTH").getOrElse("")) else None;
      val numOfWorkingHour: Option[Int] = if (priceObj != null) Option(priceObj.getAs[Int]("numOfWorkingHour").getOrElse(0)) else Some(0);

      val taskType = if (address.address.isDefined) TASK_TYPE.OFFLINE.toString else TASK_TYPE.ONLINE.toString
      val taskPrice = TaskPrice(taskResult.getAs[Boolean]("hasPriceSuggested"), Option(taskResult.getAs[String]("priceSuggested").getOrElse("")),
        taskResult.getAs[Boolean]("payPerHour"), taskResult.getAs[Int]("hoursToDo"),
        taskResult.getAs[Boolean]("repeat"),
        Option(taskResult.getAs[Int]("timesToRepeat").getOrElse(0)),
        tariffWithoutFeeForSTH, tariffWithFeeForSTH,
        if (priceObj != null) priceObj.getAs[Int]("numOfWorkingHour") else None)

      val task = Task(
        title = taskResult.getAs[String]("title").getOrElse(""),
        id = taskResult.getAs[ObjectId]("_id"),
        description = taskResult.getAs[String]("description").getOrElse(""),
        createdDate = taskResult.getAs[java.util.Date]("createdDate").get,
        address = Option(address),
        endDate = taskResult.getAs[java.util.Date]("endDate").getOrElse(new Date()),
        time = taskResult.getAs[String]("time").getOrElse(""),
        status = taskResult.getAs[String]("status").getOrElse(""),
        userId = taskResult.getAs[String]("userId").getOrElse(""),
        bids = bidss,
        comments = comms,
        distance = distance,
        category = taskResult.getAs[String]("category"),
        categoryId = taskResult.getAs[String]("categoryId"),
        taskType = taskResult.getAs[String]("type").getOrElse(taskType),
        badges = Option(badg),
        requestType = taskResult.getAs[String]("requestType").getOrElse(TASK_REQUEST_TYPE.WITH_AUCTION_ONLY.toString),
        hireSthId = taskResult.getAs[String]("hireSthId"),
        taskPrice = Option(taskPrice),
        doneBy = taskResult.getAs[Boolean]("doneBy"),
        bidAcceptedId = taskResult.getAs[String]("bidAcceptedId"),
        currency = taskResult.getAs[String]("currency")
      )
      Option(task) //return the task object

    } catch {
      case x: Exception =>
        {
          logger.debug("Problem in building the Task object for id:{}", taskResult.getAs[ObjectId]("_id"))
          logger.debug("error :{}", x)
        }
        None
    }

  }

  def deleteTask(taskId: String) {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(taskId))
    collection remove (q)
  }

  def deleteCommentAnswers(commentId: String) {
    val collection = MongoFactory.getCollection("commentAnswer")
    val query = MongoDBObject("commentId" -> commentId)
    collection remove (query)
  }

  private def buildBid(bid: DBObject, taskId: String): Bid = {

    Bid(

      createdDate = bid.getAs[Date]("created"),
      offeredValue = bid.getAs[String]("offeredValue").getOrElse("0"),
      incrementedValue = bid.getAs[String]("incrementedValue").getOrElse("0"),
      comment = bid.getAs[String]("comment").getOrElse("NOT FOUND"),
      sthId = bid.getAs[String]("taskhelperId").getOrElse("NOT FOUND"),
      sth = bid.getAs[String]("taskHelperUserName").getOrElse("NOT FOUND"),
      taskId = Option(taskId),
      id = bid.getAs[String]("_id"),
      status = bid.getAs[String]("status")

    )

  }

  private def hasCommentAnswers(commentId: String): Boolean = {

    val res = findCommentAnswerByCommentId(commentId)
    res.isDefined

  }

  private def buildComment(comment: DBObject, taskId: String): Comment = {
    val taskIdTemp = if (!taskId.isEmpty) taskId else comment.getAs[String]("taskId").getOrElse("0")
    val conversation = hasCommentAnswers(comment.getAs[String]("_id").getOrElse("NOT_"))
    Comment(

      dateCreated = comment.getAs[Date]("dateCreated").getOrElse(new Date()),
      userId = comment.getAs[String]("userId").getOrElse("0"),
      userName = comment.getAs[String]("username").getOrElse("0"),
      comment = comment.getAs[String]("comment").getOrElse("NOT FOUND"),
      taskId = taskIdTemp,
      id = comment.getAs[String]("_id"),
      status = comment.getAs[String]("status"),
      commentId = comment.getAs[String]("commentId"),
      conversation = conversation

    )

  }

  private def buildCommentAnswer(comment: DBObject, taskId: String): Comment = {
    val taskIdTemp = if (!taskId.isEmpty) taskId else comment.getAs[String]("taskId").getOrElse("0")

    Comment(

      dateCreated = comment.getAs[Date]("dateCreated").getOrElse(new Date()),
      userId = comment.getAs[String]("userId").getOrElse("0"),
      userName = comment.getAs[String]("username").getOrElse("0"),
      comment = comment.getAs[String]("comment").getOrElse("NOT FOUND"),
      taskId = taskIdTemp,
      id = Option(comment.getAs[ObjectId]("_id").get.toString),
      status = comment.getAs[String]("status"),
      commentId = comment.getAs[String]("commentId"),
      conversation = false

    )

  }

  def createBid(bid: Bid, bidId: String) = {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(bid.taskId.get))

    collection update (q, $push(("bids", buildBidDbObject(bid, bidId))))
    Response("Success", "1")
  }

  def buildBidDbObject(bid: Bid, id: String): MongoDBObject = {
    MongoDBObject(
      "created" -> new Date(),
      "offeredValue" -> bid.offeredValue,
      "incrementedValue" -> bid.incrementedValue,
      "comment" -> bid.comment,
      "taskhelperId" -> bid.sthId,
      "taskHelperUserName" -> bid.sth,
      "taskId" -> bid.taskId,
      "_id" -> id
    )
  }

  def findBids(taskId: String) = {
    findTaskById(taskId).get.bids.get.sortWith(_.createdDate.get after _.createdDate.get)
  }

  def createComment(comment: Comment, commentId: String) = {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(comment.taskId))

    collection update (q, $push(("comments", buildCommentDbObject(comment, commentId))))
    Response("Success", "1")
  }

  def createCommentAnswer(comment: CommentAnswer) = {
    val collection = MongoFactory.getCollection("commentAnswer")
    collection save (buildCommentAnswerDbObject(comment))
    Response("Success", "1")
  }

  def buildCommentDbObject(comment: Comment, id: String): MongoDBObject = {
    MongoDBObject(
      "dateCreated" -> new Date(),
      "userId" -> comment.userId,
      "username" -> comment.userName,
      "comment" -> comment.comment,
      "taskId" -> comment.taskId,
      "_id" -> id,
      "status" -> COMMENT_STATUS.VALID.toString()
    )
  }

  def buildCommentAnswerDbObject(comment: CommentAnswer): MongoDBObject = {
    MongoDBObject(
      "dateCreated" -> new Date(),
      "userId" -> comment.userId,
      "username" -> comment.userName,
      "comment" -> comment.comment,
      "taskId" -> comment.taskId,
      "commentId" -> comment.commentId,
      "status" -> COMMENT_STATUS.VALID.toString()
    )
  }

  def findTaskCategory(categoryType: Option[String]) = {
    val collection = MongoFactory.getCollection("userSkills")
    val query = MongoDBObject("type" -> categoryType.getOrElse(""))
    val result = if (categoryType.isDefined) { collection find query } else { collection find }

    TaskCategories(result.map(x => buildTaskCategory(x)).toSeq)

  }

  private def buildTaskCategory(category: DBObject): TaskCategory = {
    TaskCategory(
      id = category.getAs[ObjectId]("_id").get.toString,
      categoryType = category.getAs[String]("type"),
      title_it = category.getAs[String]("title_it"),
      title_en = category.getAs[String]("title_en"),
      description = category.getAs[String]("description"),
      order = category.getAs[Int]("order"),
      tariff = category.getAs[String]("tariff")
    )
  }

  def findComments(taskId: String) = {
    findTaskById(taskId).get.comments.get.sortWith(_.dateCreated after _.dateCreated)
  }

  def findTaskById(id: String): Option[Task] = {
    if (!ObjectId.isValid(id))
      return None

    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))

    val collection = MongoFactory.getCollection("task")
    val result = collection findOne q
    if (result != None) {
      val taskResult = result.get
      buildTask(taskResult, None)

    } else
      None
  }

  def findCommentAnswerByCommentId(id: String): Option[Comments] = {

    val q = MongoDBObject("commentId" -> id)

    val collection = MongoFactory.getCollection("commentAnswer")
    val result = collection find q sort (MongoDBObject("dateCreated" -> -1))
    if (result != None && result.size > 0) {

      Option(Comments(result.map(x => buildCommentAnswer(x, "")).toSeq))
    } else
      None
  }

  def getAddressForTasksOnline {

    val location = Location("41.87194", "12.56738")
    val address = Address(Option(""), Option(""), "ITALIA", Option(location), Option(""), Option(""))
    address
  }

  def getAddressForTaskOnlineMongodbObject: MongoDBObject = {
    MongoDBObject(

      "country" -> "IT",

      "regione" -> "",
      "address" -> "",
      "location" -> MongoDBObject(
        "latitude" -> "41.87194",
        "longitude" -> "12.56738"
      )
    )
  }

  def createTask(task: Task, language: String) = {
    val collection = MongoFactory.getCollection("task")
    val doc = if (task.address.isDefined) buildMongodBObjTaskWithAddress(task, true) else buildMongodBObjTaskWithAddress(task, false)
    collection.save(doc)

    if (task.taskType == "OFFLINE" && task.address.isDefined) {

      saveCityFromAddress(task.address.get, language)

    }

    Response("Success", doc.getAs[org.bson.types.ObjectId]("_id").get.toString)

  }

  def updateTaskStatus(taskId: String, status: String) = {
    updateTaskAttribute(taskId, $set("status" -> status))
  }

  def updateTaskAttribute(taskId: String, dbObj: DBObject): Unit = {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(taskId))
    collection update (q, dbObj)
  }

  def updateTaskStatusAndRequestType(taskId: String, status: String, requestType: String) = {
    updateTaskAttribute(taskId, $set("status" -> status, "requestType" -> requestType))

  }

  case class Price(numOfWorkingHour: Int, tariffWithoutFeeForSTH: String, tariffWithFeeForSTH: String)

  private def buildMongodBObjTaskWithAddress(task: Task, useTaskAddress: Boolean): MongoDBObject = {
    val obj = MongoDBObject(

      "createdDate" -> task.createdDate,
      "description" -> task.description,
      "title" -> task.title,
      "endDate" -> task.endDate,
      "time" -> task.time,
      "userId" -> task.userId,
      "status" -> task.status,
      "category" -> task.categoryId,
      "categoryId" -> task.categoryId,
      "type" -> task.taskType,
      "doneBy" -> task.doneBy.getOrElse(false),
      "hireSthId" -> task.hireSthId.getOrElse(""),
      "withHire" -> (if (task.hireSthId.isDefined) true else false),
      "hasPriceSuggested" -> (if (task.taskPrice.isDefined) task.taskPrice.get.hasPriceSuggested.getOrElse(false) else false),
      "priceSuggested" -> (if (task.taskPrice.isDefined) task.taskPrice.get.priceSuggested.getOrElse("0") else "0"),
      "requestType" -> task.requestType,
      "payPerHour" -> (if (task.taskPrice.isDefined) task.taskPrice.get.isPerHour.getOrElse(false) else false),
      "hoursToDo" -> (if (task.taskPrice.isDefined) task.taskPrice.get.nOfHours.getOrElse(0) else 0),
      "repeat" -> (if (task.taskPrice.isDefined) task.taskPrice.get.toRepeat.getOrElse(false) else false),
      "timesToRepeat" -> (if (task.taskPrice.isDefined) task.taskPrice.get.nOfWeeks.getOrElse(0) else 0),
      "price" -> getMongoDBObjFromTaskPrice(task),
      "postedDate" -> (if (task.requestType == TASK_REQUEST_TYPE.WITH_AUCTION_ONLY.toString) task.createdDate else null),
      "emailVerBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.emailVerBudgetRequired else false),
      "linkedInBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.linkedInBudgetRequired else false),
      "twitterBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.twitterBudgetRequired else false),
      "fbBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.fbBudgetRequired else false),
      "secDocBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.secDocBudgetRequired else false),
      "webcamBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.webcamBudgetRequired else false),
      "passportIdBudgetRequired" -> (if (task.badges.isDefined) task.badges.get.passportIdBudgetRequired else false),
      "currency" -> task.currency.getOrElse(""),
      "address" -> (if (useTaskAddress) getMongoDBObjFromAddress(task.address.get) else getAddressForTaskOnlineMongodbObject)

    )

    obj
  }

}

