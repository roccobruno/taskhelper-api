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
case class Address(address: Option[String], city: Option[String], country: String, location: Location, postcode: String, regione: Option[String])
case class Task(id: Option[ObjectId], title: String, description: String, createdDate: Date, address: Address, endDate: Date, time: String, status: String, userId: String)

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

  implicit val taskFormat = jsonFormat9(Task)
}
