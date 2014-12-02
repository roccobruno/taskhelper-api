package com.supertaskhelper.domain

import java.text.SimpleDateFormat
import java.util.Date

import com.supertaskhelper.common.enums.{TASK_REQUEST_TYPE, TASK_STATUS}
import com.supertaskhelper.domain.search.Searchable
import org.bson.types.ObjectId
import spray.json._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 03/02/2014
 * Time: 22:55
 * To change this template use File | Settings | File Templates.
 */

case class Location(longitude: String, latitude: String)
case class Bids(bids: Seq[Bid])
case class Bid(createdDate: Option[Date], offeredValue: String, incrementedValue: String, sthId: String,
    sth: String, comment: String, taskId: Option[String], id: Option[String], status: Option[String]) {

  require(!incrementedValue.isEmpty, "incremented value cannot be empty")
  require(!offeredValue.isEmpty, "offeredValue  cannot be empty")
  require(createdDate != null, "createdDate  cannot be empty")
  require(!sthId.isEmpty, "sthId  cannot be empty")
  require(!taskId.isEmpty, "taskId  cannot be empty")
  require(ObjectId.isValid(taskId.get), "taskId provided not valid")
}
case class Address(address: Option[String], city: Option[String], country: String, location: Option[Location], postcode: Option[String], regione: Option[String])

case class Task(id: Option[ObjectId], title: String, description: String, createdDate: Date, address: Option[Address], endDate: Date, time: String, status: String, userId: String,
    bids: Option[Seq[Bid]], comments: Option[Seq[Comment]], distance: Option[String], category: Option[String],
    categoryId: Option[String], taskType: String, badges: Option[TaskBadges], requestType: String, hireSthId: Option[String], taskPrice: Option[TaskPrice],
    doneBy: Option[Boolean]) extends Searchable {

  require(createdDate != null, "createdDate cannot be null")
  require(!title.isEmpty, "title cannot be empty")
  require(!description.isEmpty, "description cannot be empty")
  require(!taskType.isEmpty, "taskType cannot be empty")
  require(taskType == "ONLINE" || taskType == "OFFLINE", "taskType can be either ONLINE or OFFLINE. When OFFLINE an address must be supplied")
  require(endDate != null, "endDate cannot be empty")
  require(!time.isEmpty, "time cannot be empty")
  require(!status.isEmpty, "status cannot be empty")
  require(!TASK_STATUS.valueOf(status).toString.isEmpty, "status can be one of:" + TASK_STATUS.values())
  require(!requestType.isEmpty, "status cannot be empty")
  require(!TASK_REQUEST_TYPE.valueOf(requestType).toString.isEmpty, "requestType can only be one of:" + TASK_REQUEST_TYPE.values())
  require(status != TASK_STATUS.TOAPPROVEREQUEST.toString || (status == TASK_STATUS.TOAPPROVEREQUEST.toString && hireSthId.isDefined), "when status is TOAPPROVEREQUEST hireSthId cannot be empty")
  require(!hireSthId.isDefined || hireSthId.get.isEmpty || (ObjectId.isValid(hireSthId.get)), "the hireSthId is invalid")
  require(!userId.isEmpty && (ObjectId.isValid(userId)), "the userId is invalid")
  require(!taskPrice.isDefined || (!taskPrice.get.isPerHour.isDefined) || (!taskPrice.get.isPerHour.get) || (taskPrice.get.isPerHour.get && taskPrice.get.nOfHours.isDefined), "when isPerHour is true nOfHours cannot be empty")
  require(!taskPrice.isDefined || (!taskPrice.get.toRepeat.isDefined) || (!taskPrice.get.toRepeat.get) || (taskPrice.get.toRepeat.get && taskPrice.get.nOfWeeks.isDefined), "when toRepeat is true nOfWeeks cannot be empty")
  require(!taskPrice.isDefined || (!taskPrice.get.hasPriceSuggested.isDefined) || (!taskPrice.get.hasPriceSuggested.get) || (taskPrice.get.hasPriceSuggested.get && taskPrice.get.priceSuggested.isDefined), "when hasPriceSuggested is true priceSuggested cannot be empty")
  require(requestType != TASK_REQUEST_TYPE.WITH_DIRECT_HIRE_AND_TARIFF.toString || (
    requestType == TASK_REQUEST_TYPE.WITH_DIRECT_HIRE_AND_TARIFF.toString && taskPrice.isDefined && taskPrice.get.nOfHours.isDefined
    && taskPrice.get.tariffWithFeeForSth.isDefined && taskPrice.get.tariffWithoutFeeForSth.isDefined
  ), "when requestType is WITH_AUCTION_FROM_DIRECT_HIRE_WITH_TARIFF nOfHours,tariffWithFeeForSth and tariffWithoutFeeForSth cannot be empty ")
}

case class TaskBadges(emailVerBudgetRequired: Option[Boolean],
  fbBudgetRequired: Option[Boolean], linkedInBudgetRequired: Option[Boolean], passportIdBudgetRequired: Option[Boolean],
  secDocBudgetRequired: Option[Boolean], twitterBudgetRequired: Option[Boolean], webcamBudgetRequired: Option[Boolean])

case class TaskPrice(hasPriceSuggested: Option[Boolean], priceSuggested: Option[String],
  isPerHour: Option[Boolean], nOfHours: Option[Int], toRepeat: Option[Boolean], nOfWeeks: Option[Int], tariffWithoutFeeForSth: Option[String],
  tariffWithFeeForSth: Option[String])

object TaskPriceJsonFormat extends DefaultJsonProtocol {
  implicit val taskPriceJsonFormat = jsonFormat8(TaskPrice)
}
object TaskBadgesJsonFormat extends DefaultJsonProtocol {
  implicit val taskBadgesJsonFormat = jsonFormat7(TaskBadges)
}

case class Comment(id: Option[String], userId: String, userName: String, comment: String, dateCreated: Date, taskId: String, status: Option[String], commentId: Option[String], conversation: Boolean) {
  require(!comment.isEmpty, "comment  cannot be empty")
  require(dateCreated != null, "createdDate  cannot be empty")
  require(!userId.isEmpty, "userId  cannot be empty")
  require(!taskId.isEmpty, "taskId  cannot be empty")
  require(!userName.isEmpty, "userName  cannot be empty")
  require(ObjectId.isValid(taskId), "taskId provided not valid")
}

case class CommentAnswer(id: Option[String], userId: String, userName: String, comment: String, dateCreated: Date, taskId: String, status: Option[String], commentId: Option[String]) {
  require(!comment.isEmpty, "comment  cannot be empty")
  require(dateCreated != null, "createdDate  cannot be empty")
  require(!userId.isEmpty, "userId  cannot be empty")
  require(!taskId.isEmpty, "taskId  cannot be empty")
  require(!userName.isEmpty, "userName  cannot be empty")
  require(ObjectId.isValid(taskId), "taskId provided not valid")
  require(!commentId.isEmpty, "commentId  cannot be empty")
}

case class Comments(comments: Seq[Comment])
object CommentAnswerJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val commentAnswerFormat = jsonFormat8(CommentAnswer)
}

object CommentJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val commentFormat = jsonFormat9(Comment)
}

object CommentsJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
        dateStringFormat.parse(value)
      }

      case _ => deserializationError("Date expected")
    }
  }
  implicit val commentFormat = jsonFormat9(Comment)
  implicit val commentsFormat = jsonFormat1(Comments)
}

object BidJsonFormat extends DefaultJsonProtocol {
  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(c: Date) = {
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
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
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
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
      val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
      JsString(dateStringFormat.format(c))
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {
        val dateStringFormat = new SimpleDateFormat(Constants.DATE_FORMAT)
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
  implicit val commentFormat = jsonFormat9(Comment)
  implicit val taskBadgesFormat = jsonFormat7(TaskBadges)
  implicit val taskPriceFormat = jsonFormat8(TaskPrice)

  implicit val taskFormat = jsonFormat20(Task)
}

case class TaskParams(id: Option[String],
  status: Option[String],
  tpId: Option[String],
  sthId: Option[String],
  sort: Option[String],
  city: Option[String],
  page: Option[Int],
  sizePage: Option[Int],
  distance: Option[String],
  language: Option[String],
  bidSthId: Option[String],
  hireSthId: Option[String]) extends Pagination(page, sizePage)

object TaskParamsFormat extends DefaultJsonProtocol {
  implicit val taskParamsFormat = jsonFormat12(TaskParams)
}

case class UpdateTaskStatusParams(id: String, status: String, language: Option[String]) {

  require(!TASK_STATUS.valueOf(status).toString.isEmpty, "status can be one of:" + TASK_STATUS.values())
}

object UpdateTaskStatusParamsFormat extends DefaultJsonProtocol {
  implicit val updateTaskStatusParamsFormat = jsonFormat3(UpdateTaskStatusParams)
}

case class Tasks(tasks: Seq[Task])
object TasksJsonFormat extends DefaultJsonProtocol {
  import com.supertaskhelper.domain.TaskJsonFormat._
  implicit val taskFormat = jsonFormat20(Task)
  implicit val tasksFormat = jsonFormat1(Tasks)
}