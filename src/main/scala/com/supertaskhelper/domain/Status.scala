package com.supertaskhelper.domain

import spray.json.DefaultJsonProtocol

case class Status(status: String)

object StatusJsonFormat extends DefaultJsonProtocol {
  implicit val statusFormat = jsonFormat1(Status)
}