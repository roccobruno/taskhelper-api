package com.supertaskhelper.domain

import spray.json._
import java.util.Date

import java.text.SimpleDateFormat
import org.bson.types.ObjectId
import com.supertaskhelper.domain.search.Searchable

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:55
 * To change this template use File | Settings | File Templates.
 */

case class Location(longitude: String, latitude: String)
case class Bids(bids: Seq[Bid])
case class Bid(createdDate: Date, offeredValue: String, incrementedValue: String, sthId: String,
    sth: String, comment: String, taskId: Option[String], id: Option[String], status: Option[String]) {

  require(!incrementedValue.isEmpty, "incremented value cannot be empty")
  require(!offeredValue.isEmpty, "offeredValue  cannot be empty")
  require(createdDate != null, "createdDate  cannot be empty")
  require(!sthId.isEmpty, "sthId  cannot be empty")
  require(!taskId.isEmpty, "taskId  cannot be empty")
}
case class Address(address: Option[String], city: Option[String], country: String, location: Option[Location], postcode: String, regione: Option[String])
case class Task(id: Option[ObjectId], title: String, description: String, createdDate: Date, address: Address, endDate: Date, time: String, status: String, userId: String,
  bids: Option[Seq[Bid]], comments: Option[Seq[Comment]], distance: Option[String], category: Option[String],
  categoryId: Option[String]) extends Searchable

case class Comment(id: Option[String], userId: String, userName: String, comment: String, dateCreated: Date, taskId: String, status: Option[String]) {
  require(!comment.isEmpty, "comment  cannot be empty")
  require(dateCreated != null, "createdDate  cannot be empty")
  require(!userId.isEmpty, "userId  cannot be empty")
  require(!taskId.isEmpty, "taskId  cannot be empty")
  require(!userName.isEmpty, "userName  cannot be empty")
}

case class Comments(comments: Seq[Comment])

object CommentJsonFormat extends DefaultJsonProtocol {
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
  implicit val commentFormat = jsonFormat7(Comment)
}

object CommentsJsonFormat extends DefaultJsonProtocol {
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
  implicit val commentFormat = jsonFormat7(Comment)
  implicit val commentsFormat = jsonFormat1(Comments)
}

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
  implicit val bidFormat = jsonFormat9(Bid)
}

object BidsJsonFormat extends DefaultJsonProtocol {
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
  implicit val bidFormat = jsonFormat9(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)
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
  implicit val bidFormat = jsonFormat9(Bid)
  implicit val commentFormat = jsonFormat7(Comment)
  implicit val taskFormat = jsonFormat14(Task)
}

case class TaskParams(id: Option[String], status: Option[String], tpId: Option[String], sthId: Option[String],
  sort: Option[String], city: Option[String], page: Option[Int], sizePage: Option[Int], distance: Option[String]) extends Pagination(page, sizePage)

object TaskParamsFormat extends DefaultJsonProtocol {
  implicit val taskParamsFormat = jsonFormat9(TaskParams)
}

case class Tasks(tasks: Seq[Task])
object TasksJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.TaskJsonFormat._
  implicit val taskFormat = jsonFormat14(Task)
  implicit val tasksFormat = jsonFormat1(Tasks)
}