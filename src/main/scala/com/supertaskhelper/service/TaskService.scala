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

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 23:23
 * To change this template use File | Settings | File Templates.
 */
trait TaskService {

  val conn = MongoFactory.getConnection

  def findTask(id: String) = {
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))
    val collection = MongoFactory.getCollection("task")
    val result = collection findOne q

    val taskResult = result.get

    val addobj = taskResult.get("address").asInstanceOf[BasicDBObject]
    val locationObj = addobj.get("location").asInstanceOf[BasicDBObject]
    val location = if (locationObj != null) { Location(locationObj.getString("longitude"), locationObj.getString("latitude")) } else null

    val address = Address(Option(addobj.getString("address")), Option(addobj.getString("city")), addobj.getString("country"), location, addobj.getString("postcode"), Option(addobj.getString("regione")))
    import _root_.scala.collection.JavaConverters._
    val task = Task(
      title = taskResult.get("title").toString,
      id = taskResult.getAs[ObjectId]("_id"),
      description = taskResult.get("description").toString,
      createdDate = taskResult.getAs[java.util.Date]("createdDate").get,
      address = address,
      endDate = taskResult.getAs[java.util.Date]("endDate").get,
      time = taskResult.get("time").toString,
      status = taskResult.get("status").toString,
      userId = taskResult.get("userId").toString)

    task //return the task object
  }

  def createTask(task: Task) = {
    val collection = MongoFactory.getCollection("task")
    //    val taskn = Task(Option(new ObjectId), task.title, task.description, task.createdDate, task.address, task.endDate, task.time, task.status, task.userId)
    import com.supertaskhelper.domain.TaskJsonFormat._
    import spray.json._
    import DefaultJsonProtocol._
    //    val taskn = Task(Option(new ObjectId), task.title, task.description, task.createdDate, task.address, task.endDate, task.time, task.status, task.userId).toJson
    val taskn = task.toJson

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
    Response("Sussess", "1")

  }

}

