package com.supertaskhelper.service

import com.supertaskhelper.MongoFactory
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.domain._
import com.supertaskhelper.domain.TaskJsonFormat._
import com.mongodb.casbah.commons.conversions.MongoConversionHelper
import org.bson.{ BSON, Transformer }
import com.mongodb.casbah.Imports._
import com.mongodb.BasicDBObject
import com.supertaskhelper.domain.Response
import com.supertaskhelper.domain.Location
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Address
import com.supertaskhelper.domain.ResponseJsonFormat._
import java.util.Date
import com.supertaskhelper.common.enums.COMMENT_STATUS
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import com.supertaskhelper.util.ConverterUtil

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 23:23
 * To change this template use File | Settings | File Templates.
 */
trait TaskService extends Service with ConverterUtil {

  val conn = MongoFactory.getConnection

  def findTask(params: TaskParams): Seq[Task] = {
    println("Build Task with params:{}", params)
    val q = buildQuery(params)
    //    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(params.id.get))
    val collection = MongoFactory.getCollection("task")
    (collection find q).sort(MongoDBObject("createdDate" -> -1))
      .skip((params.page.getOrElse(1) - 1) * params.sizePage.getOrElse(10))
      .limit(params.sizePage.getOrElse(10)).map(x => buildTask(x, params.distance)).toSeq
  }

  private def buildQuery(params: TaskParams): DBObject = {
    val builder = MongoDBObject.newBuilder
    if (params.id.isDefined && ObjectId.isValid(params.id.get))
      builder += "_id" -> new org.bson.types.ObjectId(params.id.get)

    if (params.city.isDefined)
      builder += "address.city" -> params.city.get

    if (params.status.isDefined)
      builder += "status" -> params.status.get

    if (params.tpId.isDefined)
      builder += "userId" -> params.tpId.get

    if (params.sthId.isDefined)
      builder += "taskHelperId" -> params.sthId.get

    builder.result
  }

  private def buildTask(taskResult: DBObject, distance: Option[String]): Task = {

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

    val address = Address(Option(addobj.getString("address")), Option(addobj.getString("city")), addobj.getString("country"), location, addobj.getString("postcode"), Option(addobj.getString("regione")))
    import _root_.scala.collection.JavaConverters._
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
      taskType = taskResult.getAs[String]("type").get,
      emailVerBudgetRequired = taskResult.getAs[Boolean]("emailVerBudgetRequired"),
      linkedInBudgetRequired = taskResult.getAs[Boolean]("linkedInBudgetRequired"),
      fbBudgetRequired = taskResult.getAs[Boolean]("fbBudgetRequired"),
      passportIdBudgetRequired = taskResult.getAs[Boolean]("passportIdBudgetRequired"),
      twitterBudgetRequired = taskResult.getAs[Boolean]("twitterBudgetRequired"),
      secDocBudgetRequired = taskResult.getAs[Boolean]("secDocBudgetRequired"),
      webcamBudgetRequired = taskResult.getAs[Boolean]("webcamBudgetRequired")

    )
    task //return the task object
  }

  def deleteTask(taskId: String) {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(taskId))
    collection remove (q)
  }

  def deleteCommentAnswers(commentId:String) {
    val collection = MongoFactory.getCollection("commentAnswer")
    val query = MongoDBObject("commentId" -> commentId)
    collection remove(query)
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

  private def hasCommentAnswers(commentId:String):Boolean ={

     val res = findCommentAnswerByCommentId(commentId)
     res.isDefined

  }

  private def buildComment(comment: DBObject, taskId: String): Comment = {
    val taskIdTemp = if(!taskId.isEmpty ) taskId else comment.getAs[String]("taskId").getOrElse("0")
    val conversation = hasCommentAnswers(comment.getAs[String]("id").get)
    Comment(

      dateCreated = comment.getAs[Date]("dateCreated").getOrElse(new Date()),
      userId = comment.getAs[String]("userId").getOrElse("0"),
      userName = comment.getAs[String]("username").getOrElse("0"),
      comment = comment.getAs[String]("comment").getOrElse("NOT FOUND"),
      taskId = taskIdTemp,
      id = comment.getAs[String]("id"),
      status = comment.getAs[String]("status"),
      commentId =comment.getAs[String]("commentId"),
      conversation = conversation

    )

  }

  private def buildCommentAnswer(comment: DBObject, taskId: String): Comment = {
    val taskIdTemp = if(!taskId.isEmpty ) taskId else comment.getAs[String]("taskId").getOrElse("0")

    Comment(

      dateCreated = comment.getAs[Date]("dateCreated").getOrElse(new Date()),
      userId = comment.getAs[String]("userId").getOrElse("0"),
      userName = comment.getAs[String]("username").getOrElse("0"),
      comment = comment.getAs[String]("comment").getOrElse("NOT FOUND"),
      taskId = taskIdTemp,
      id = Option(comment.getAs[ObjectId]("_id").get.toString),
      status = comment.getAs[String]("status"),
      commentId =comment.getAs[String]("commentId"),
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

  def createCommentAnswer(comment:CommentAnswer)  = {
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
      "id" -> id,
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
      order = category.getAs[Int]("order")
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

      Option(buildTask(taskResult, None))
    } else
      None
  }

  def findCommentAnswerByCommentId(id: String): Option[Comments] = {

    val q = MongoDBObject("commentId" -> id)

    val collection = MongoFactory.getCollection("commentAnswer")
    val result = collection find q sort(MongoDBObject("dateCreated" -> -1))
    if (result != None && result.size>0) {


      Option(Comments(result.map(x => buildCommentAnswer(x,"")).toSeq))
    } else
      None
  }

  def createTask(task: Task) = {
    val collection = MongoFactory.getCollection("task")
    val doc = if (task.address != null) buildMongodBObjTaskWithAddress(task) else buildMongodBObjTaskWithoutAddress(task)
    collection.save(doc)

    Response("Success", doc.getAs[org.bson.types.ObjectId]("_id").get.toString)

  }
  private def buildMongodBObjTaskWithAddress(task: Task): MongoDBObject = {
    val obj = MongoDBObject(

      "createdDate" -> task.createdDate,
      "description" -> task.description,
      "title" -> task.title,
      "endDate" -> task.endDate,
      "time" -> task.time,
      "userId" -> task.userId,
      "status" -> task.status,
      "category" -> task.category,
      "categoryId" -> task.categoryId,
      "type" -> task.taskType,
      "emailVerBudgetRequired" -> task.emailVerBudgetRequired,
      "linkedInBudgetRequired" -> task.linkedInBudgetRequired,
      "twitterBudgetRequired" -> task.twitterBudgetRequired,
      "fbBudgetRequired" -> task.fbBudgetRequired,
      "secDocBudgetRequired" -> task.secDocBudgetRequired,
      "webcamBudgetRequired" -> task.webcamBudgetRequired,
      "passportIdBudgetRequired" -> task.passportIdBudgetRequired,

      "address" -> getMongoDBObjFromAddress(task.address.get)

    )

    obj
  }

  private def buildMongodBObjTaskWithoutAddress(task: Task): MongoDBObject = {
    val obj = MongoDBObject(

      "createdDate" -> task.createdDate,
      "description" -> task.description,
      "title" -> task.title,
      "endDate" -> task.endDate,
      "time" -> task.time,
      "userId" -> task.userId,
      "status" -> task.status,
      "category" -> task.category,
      "categoryId" -> task.categoryId,
      "type" -> task.taskType,
      "emailVerBudgetRequired" -> task.emailVerBudgetRequired,
      "linkedInBudgetRequired" -> task.linkedInBudgetRequired,
      "twitterBudgetRequired" -> task.twitterBudgetRequired,
      "fbBudgetRequired" -> task.fbBudgetRequired,
      "secDocBudgetRequired" -> task.secDocBudgetRequired,
      "webcamBudgetRequired" -> task.webcamBudgetRequired,
      "passportIdBudgetRequired" -> task.passportIdBudgetRequired

    )
    obj
  }

}

