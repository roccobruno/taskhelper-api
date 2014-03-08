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

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 23:23
 * To change this template use File | Settings | File Templates.
 */
trait TaskService extends Service {

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
    if (params.id.isDefined)
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

    val bidss: Seq[Bid] = taskResult.get("bids").asInstanceOf[BasicDBList].map(x => buildBid(x.asInstanceOf[BasicDBObject], taskResult.getAs[ObjectId]("_id").get.toString)).toSeq.sortWith(_.createdDate after _.createdDate)
    val comms: Seq[Comment] = taskResult.get("comments").asInstanceOf[BasicDBList].map(x => buildComment(x.asInstanceOf[BasicDBObject], taskResult.getAs[ObjectId]("_id").get.toString)).toSeq.sortWith(_.dateCreated after _.dateCreated)
    val addobj = taskResult.get("address").asInstanceOf[BasicDBObject]
    val locationObj = addobj.get("location").asInstanceOf[BasicDBObject]
    val location = if (locationObj != null) { Location(locationObj.getString("longitude"), locationObj.getString("latitude")) } else null

    val address = Address(Option(addobj.getString("address")), Option(addobj.getString("city")), addobj.getString("country"), location, addobj.getString("postcode"), Option(addobj.getString("regione")))
    import _root_.scala.collection.JavaConverters._
    val task = Task(
      title = taskResult.getAs[String]("title").getOrElse(""),
      id = taskResult.getAs[ObjectId]("_id"),
      description = taskResult.getAs[String]("description").getOrElse(""),
      createdDate = taskResult.getAs[java.util.Date]("createdDate").get,
      address = address,
      endDate = taskResult.getAs[java.util.Date]("endDate").getOrElse(new Date()),
      time = taskResult.getAs[String]("time").getOrElse(""),
      status = taskResult.getAs[String]("status").getOrElse(""),
      userId = taskResult.getAs[String]("userId").getOrElse(""),
      bids = Option(bidss),
      comments = Option(comms),
      distance = distance)

    task //return the task object
  }

  private def buildBid(bid: DBObject, taskId: String): Bid = {

    Bid(

      createdDate = bid.getAs[Date]("created").getOrElse(new Date()),
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

  private def buildComment(comment: DBObject, taskId: String): Comment = {

    Comment(

      dateCreated = comment.getAs[Date]("dateCreated").getOrElse(new Date()),
      userId = comment.getAs[String]("userId").getOrElse("0"),
      userName = comment.getAs[String]("username").getOrElse("0"),
      comment = comment.getAs[String]("comment").getOrElse("NOT FOUND"),
      taskId = taskId,
      id = comment.getAs[String]("id"),
      status = comment.getAs[String]("status")

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
    findTaskById(taskId).bids.get.sortWith(_.createdDate after _.createdDate)
  }

  def createComment(comment: Comment, commentId: String) = {
    val collection = MongoFactory.getCollection("task")
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(comment.taskId))

    collection update (q, $push(("comments", buildCommentDbObject(comment, commentId))))
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

  def findCommentss(taskId: String) = {
    findTaskById(taskId).comments.get.sortWith(_.dateCreated after _.dateCreated)
  }

  def findTaskById(id: String) = {
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))

    val collection = MongoFactory.getCollection("task")
    val result = collection findOne q
    if (result != None) {
      val taskResult = result.get

      buildTask(taskResult, None)
    } else
      null
  }

  def createTask(task: Task) = {
    val collection = MongoFactory.getCollection("task")
    val doc = MongoDBObject(

      "createdDate" -> task.createdDate,
      "description" -> task.description,
      "address" -> MongoDBObject(

        "address" -> task.address.address,
        "location" -> MongoDBObject(
          "latitude" -> task.address.location.latitude
        )))
    collection.save(doc)

    Response("Success", doc.getAs[org.bson.types.ObjectId]("_id").get.toString)

  }

}

