package com.supertaskhelper.domain

import spray.json._
import java.util.{ Date, Locale }
import java.text.SimpleDateFormat
import com.supertaskhelper.domain.UserRegistration
import com.supertaskhelper.domain.User

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:40
 * To change this template use File | Settings | File Templates.
 */
case class User(userName: String, isSTH: Boolean, email: String, password: String, id: String)

object UserJsonFormat extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat5(User)
}

case class UserRegistration(userName: String, password: String, confirmPassword: String, email: String,
    language: Option[Locale]) {

  require(!userName.isEmpty, "username must not be empty")
  require(!email.isEmpty, "email must not be empty")
  require(!password.isEmpty, "password must not be empty")
  require(!confirmPassword.isEmpty, "confirmPassword must not be empty")
  require(password == confirmPassword, "password and confirmpassword must be equals")

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
  implicit val userRegistrationFormat = jsonFormat5(UserRegistration)
}