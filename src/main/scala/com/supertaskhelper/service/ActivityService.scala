package com.supertaskhelper.service

import com.supertaskhelper.MongoFactory
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.domain.{ User, Activity }
import com.mongodb.casbah.Imports._
import java.util.Date
import com.supertaskhelper.domain.search.ActivityParams

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 09/03/2014
 * Time: 19:09
 * To change this template use File | Settings | File Templates.
 */
trait ActivityService extends Service with UserService with TaskService {

  def findLastActivities(params: ActivityParams): Seq[Activity] = {

    var collection = MongoFactory.getCollection("activity")
    val startingPos = params.positionId.getOrElse(-1)
    val query = if (params.subjectId.isDefined) MongoDBObject("subjectId" -> params.subjectId.getOrElse("none"))
    else ("positionId" $gte startingPos)

    (collection find query).sort(MongoDBObject("positionId" -> -1)).limit(params.sizePage.getOrElse(10)).
      skip((params.page.getOrElse(1) - 1) * params.sizePage.getOrElse(10)).map(x => buildActivity(x)).toSeq

  }

  private def buildActivity(activity: DBObject): Activity = {
    val user = findUserById(activity.getAs[String]("subjectId").get)
    var userObj = if (user._1) user._2 else null

    val task = if (activity.getAs[String]("objectId").isDefined &&
      !activity.getAs[String]("objectId").get.isEmpty) findTaskById(activity.getAs[String]("objectId").get) else None
    var objDet = if (task.isDefined) task.get.title else "TASK NOT FOUND"
    Activity(
      id = activity.getAs[ObjectId]("_id").get.toString,
      subjectId = activity.getAs[String]("subjectId").get,
      subjectUsername = if (userObj != null) userObj.userName else "USER NOT FOUND",
      activityType = activity.getAs[String]("type").get,
      createdDate = activity.getAs[Date]("dateCreation").get,
      objectId = activity.getAs[String]("objectId"),
      objectDetails = Option(objDet),
      positionId = activity.getAs[Int]("positionId").get
    )
  }

}
