package com.supertaskhelper.domain.search

import spray.json.DefaultJsonProtocol

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 17:08
 * To change this template use File | Settings | File Templates.
 */
case class UserSearchParams(id: Option[String], email: Option[String]) {

  require((!id.isEmpty && email.isEmpty) || (id.isEmpty && !email.isEmpty), "either id or email must have a value")

}

object UserSearchParamsJsonFormat extends DefaultJsonProtocol {
  implicit val userSearchParamsFormat = jsonFormat2(UserSearchParams)
}
