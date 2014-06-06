package com.supertaskhelper.domain

import spray.json._
import java.util.{ Date, Locale }
import java.text.SimpleDateFormat
import com.supertaskhelper.domain.UserRegistration
import com.supertaskhelper.domain.User
import com.supertaskhelper.common.enums.SOURCE
import com.supertaskhelper.domain.search.Searchable
import spray.json._
import DefaultJsonProtocol._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:40
 * To change this template use File | Settings | File Templates.
 */

case class User(userName: String, lastName: String, isSTH: Boolean, email: String, password: String, id: String, imgUrl: Option[String],
  distance: Option[String], address: Option[Address], bio: Option[String], fbBudget: Option[Boolean], twitterBudget: Option[Boolean],
  linkedInBudget: Option[Boolean], securityDocVerified: Option[Boolean], emailVerified: Option[Boolean], idDocVerified: Option[Boolean],
  webcamVerified: Option[Boolean], accountStatus: Option[String])
    extends Searchable
object UserJsonFormat extends DefaultJsonProtocol {
  implicit val locationFormat = jsonFormat2(Location)
  implicit val addressFormat = jsonFormat6(Address)
  implicit val userFormat = jsonFormat18(User)
}

case class UserRegistration(userName: String, lastname: String, password: String, confirmPassword: String, email: String,
    language: Option[Locale], source: Option[String], address: Option[Address]) {

  require(!userName.isEmpty, "username must not be empty")
  require(!lastname.isEmpty, "lastname must not be empty")
  require(!email.isEmpty, "email must not be empty")
  require(!password.isEmpty, "password must not be empty")
  require(!confirmPassword.isEmpty, "confirmPassword must not be empty")
  require(password == confirmPassword, "password and confirmpassword must be equals")
  require(source.isEmpty || SOURCE.valueOf(source.get) != null, "source value not recognized")
  require(address.isEmpty || (address.get.location.isDefined), "Address must have a geolocation")

}

object UserRegistrationJsonFormat extends DefaultJsonProtocol {
  implicit object LocaleFormat extends RootJsonFormat[Locale] {
    def write(c: Locale) = {

      JsString(c.toString)
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {

        value match {
          case "it" => Locale.ITALIAN
          case "en" => Locale.UK
          case _ => Locale.ITALIAN
        }

      }

      case _ => deserializationError("Locale expected")
    }
  }
  implicit val locationFormat = jsonFormat2(Location)
  implicit val addressFormat = jsonFormat6(Address)
  implicit val userRegistrationFormat = jsonFormat8(UserRegistration)
}