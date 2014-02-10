package com.supertaskhelper.search

import spray.json.DefaultJsonProtocol
import com.supertaskhelper.search.SearchSolrCoreActor.{ SearchResultsSolrWrapper, SearchResults, SolrSearchDoc }

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:48
 * To change this template use File | Settings | File Templates.
 */
import com.supertaskhelper.domain.TaskJsonFormat._
trait SaladJsonProtocol extends DefaultJsonProtocol {

  implicit val searchResultList = jsonFormat1(SearchResultList.apply)

  //  implicit val error = jsonFormat1(ErrorDto.apply)
}

trait SolrSearchJsonProtocol extends SnakifiedSprayJsonSupport {
  val blacklist = Set("numFound")

  implicit val solrDoc = jsonFormat2(SolrSearchDoc.apply)
  implicit val solrRes = jsonFormat2(SearchResults.apply)
  implicit val solrResWrapper = jsonFormat1(SearchResultsSolrWrapper.apply)
}