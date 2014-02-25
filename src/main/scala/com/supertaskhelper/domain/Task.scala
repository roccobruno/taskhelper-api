package com.supertaskhelper.domain

import spray.json._
import java.util.Date

import java.text.SimpleDateFormat
import org.bson.types.ObjectId

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:55
 * To change this template use File | Settings | File Templates.
 */

case class Location(longitude: String, latitude: String)
case class Bid(createdDate: Date, offeredValue: String, incrementedValue: String, sthId: String, sth: String, comment: String)
case class Address(address: Option[String], city: Option[String], country: String, location: Location, postcode: String, regione: Option[String])
case class Task(id: Option[ObjectId], title: String, description: String, createdDate: Date, address: Address, endDate: Date, time: String, status: String, userId: String,
  bids: Option[Seq[Bid]])

object BidJsonFormat extends DefaultJsonProtocol {
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
  implicit val bidFormat = jsonFormat6(Bid)
}

object TaskJsonFormat extends DefaultJsonProtocol {

  implicit val locationFormat = jsonFormat2(Location)
  implicit val addressFormat = jsonFormat6(Address)

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
  implicit object IdFormat extends RootJsonFormat[ObjectId] {
    def write(c: ObjectId) = {
      JsString(c.toString)
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        new ObjectId(value.toString)
      }

      case _ => deserializationError("ObjectId expected")
    }
  }
  implicit val bidFormat = jsonFormat6(Bid)
  implicit val taskFormat = jsonFormat10(Task)
}

case class TaskParams(id: Option[String], status: Option[String], tpId: Option[String], sthId: Option[String], sort: Option[String], city: Option[String], page: Option[Int], sizePage: Option[Int]) extends Pagination(page, sizePage)

object TaskParamsFormat extends DefaultJsonProtocol {
  implicit val taskParamsFormat = jsonFormat8(TaskParams)
}

case class Tasks(tasks: Seq[Task])
object TasksJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.TaskJsonFormat._
  implicit val taskFormat = jsonFormat10(Task)
  implicit val tasksFormat = jsonFormat1(Tasks)
}