package com.supertaskhelper.domain

import java.util.Date
import spray.json._
import java.text.SimpleDateFormat
import com.supertaskhelper.domain.Activities
import com.supertaskhelper.domain.Activity

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 09/03/2014
 * Time: 19:05
 * To change this template use File | Settings | File Templates.
 */
case class Activity(id: String, subjectId: String, subjectUsername: String, activityType: String, createdDate: Date, objectId: Option[String],
  objectDetails: Option[String], positionId: Int)

case class Activities(activities: Seq[Activity])

object ActivityJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat("dd/MM/yyyy")
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val activityFormat = jsonFormat8(Activity)
}

object ActivitiesJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.ActivityJsonFormat._
  implicit val activityFormat = jsonFormat8(Activity)
  implicit val activitiesFormat = jsonFormat1(Activities)
}
