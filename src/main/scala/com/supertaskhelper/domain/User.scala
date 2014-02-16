package com.supertaskhelper.domain

import spray.json.DefaultJsonProtocol

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:40
 * To change this template use File | Settings | File Templates.
 */
case class User(userName: String, isSTH: Boolean, email: String, password: String)

object UserJsonFormat extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat4(User)
}

case class UserRegistration(userName: String, password: String, confirmPassword: String, email: String) {

  require(!userName.isEmpty, "username must not be empty")
  require(!email.isEmpty, "email must not be empty")
  require(!password.isEmpty, "password must not be empty")
  require(!confirmPassword.isEmpty, "confirmPassword must not be empty")
  require(password == confirmPassword, "password and confirmpassword must be equals")

}

object UserRegistrationJsonFormat extends DefaultJsonProtocol {
  implicit val userRegistrationFormat = jsonFormat4(UserRegistration)
}