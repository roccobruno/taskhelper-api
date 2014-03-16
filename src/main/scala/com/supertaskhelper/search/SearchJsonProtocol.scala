package com.supertaskhelper.search

import spray.json.{ JsString, JsValue, RootJsonFormat, DefaultJsonProtocol }
import com.supertaskhelper.search.SearchSolrCoreActor.{ SearchResultsSolrWrapper, SearchResults, SolrSearchDoc }
import com.supertaskhelper.domain.search.Searchable
import com.supertaskhelper.domain.{ User, Task }
import com.supertaskhelper.domain.TaskJsonFormat._
import com.supertaskhelper.domain.UserJsonFormat._
import spray.json._
import DefaultJsonProtocol._
import com.supertaskhelper.search.SearchResultListJsonFormat._

import com.supertaskhelper.search.SearchSolrCoreActor.SolrSearchDocFormat.SolrSearchDocJsonFormat

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */
import com.supertaskhelper.domain.TaskJsonFormat._
trait SaladJsonProtocol extends DefaultJsonProtocol {

  implicit val searchResultList = jsonFormat2(SearchResultList.apply)

  //  implicit val error = jsonFormat1(ErrorDto.apply)
}

trait SearchJsonProtocol extends SnakifiedSprayJsonSupport {
  val blacklist = Set("numFound")
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

  implicit val solrDoc: JsonFormat[SolrSearchDoc] = SolrSearchDocJsonFormat
  implicit val solrRes = jsonFormat2(SearchResults.apply)
  implicit val solrResWrapper = jsonFormat1(SearchResultsSolrWrapper.apply)
}