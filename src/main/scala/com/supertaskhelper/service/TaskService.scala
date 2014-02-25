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
      .limit(params.sizePage.getOrElse(10)).map(x => buildTask(x)).toSeq
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

  private def buildTask(taskResult: DBObject): Task = {

    val addobj = taskResult.get("address").asInstanceOf[BasicDBObject]
    val locationObj = addobj.get("location").asInstanceOf[BasicDBObject]
    val bidss: Seq[Bid] = taskResult.get("bids").asInstanceOf[BasicDBList].map(x => buildBid(x.asInstanceOf[BasicDBObject])).toSeq
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
      bids = Option(bidss))

    task //return the task object
  }

  private def buildBid(bid: DBObject): Bid = {

    Bid(

      createdDate = bid.getAs[Date]("created").getOrElse(new Date()),
      offeredValue = bid.getAs[String]("offeredValue").getOrElse("0"),
      incrementedValue = bid.getAs[String]("incrementedValue").getOrElse("0"),
      comment = bid.getAs[String]("String").getOrElse(""),
      sthId = bid.getAs[String]("taskhelperId").getOrElse(""),
      sth = bid.getAs[String]("taskHelperUserName").getOrElse("")

    )

  }

  def findTaskById(id: String) = {
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))

    val collection = MongoFactory.getCollection("task")
    val result = collection findOne q
    if (result != None) {
      val taskResult = result.get

      buildTask(taskResult)
    } else
      null
  }

  def createTask(task: Task) = {
    val collection = MongoFactory.getCollection("task")

    collection.save(MongoDBObject(

      "createdDate" -> task.createdDate,
      "description" -> task.description,
      "address" -> MongoDBObject(

        "address" -> task.address.address,
        "location" -> MongoDBObject(
          "latitude" -> task.address.location.latitude
        )
      )

    ))
    Response("Success", "1")

  }

}

