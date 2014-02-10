package com.supertaskhelper.domain

import spray.json._
/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 06/02/2014
 * Time: 22:52
 * To change this template use File | Settings | File Templates.
 */
case class Response(message: String, id: String)

object ResponseJsonFormat extends DefaultJsonProtocol {
  implicit val responseFormat = jsonFormat2(Response)
}
