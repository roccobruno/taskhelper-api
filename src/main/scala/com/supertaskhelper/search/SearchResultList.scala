package com.supertaskhelper.search

import com.supertaskhelper.domain.{ User, Task }
import spray.json.{ JsString, JsValue, RootJsonFormat, DefaultJsonProtocol }
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.domain.UserJsonFormat._
import com.supertaskhelper.domain.search.Searchable
import spray.json._
import DefaultJsonProtocol._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 10/02/2014
 * Time: 23:24
 * To change this template use File | Settings | File Templates.
 */
case class SearchResultList(tasks: Seq[Searchable], users: Seq[Searchable])

object SearchResultListJsonFormat extends DefaultJsonProtocol {
  implicit object SearchableJsonFormat extends RootJsonFormat[Searchable] {
    def write(a: Searchable) = a match {
      case p: Task => p.toJson
      case u: User => u.toJson
    }
    def read(value: JsValue) =
      // If you need to read, you will need something in the
      // JSON that will tell you which subclass to use
      value.asJsObject.fields("type") match {
        case JsString("TASK") => value.convertTo[Task]
        case JsString("USER") => value.convertTo[User]
      }
  }
  implicit val searchResultListFormat = jsonFormat2(SearchResultList)
}