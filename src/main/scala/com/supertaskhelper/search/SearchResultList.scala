package com.supertaskhelper.search

import com.supertaskhelper.domain.Task
import spray.json.DefaultJsonProtocol
import com.supertaskhelper.domain.TaskJsonFormat._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 10/02/2014
 * Time: 23:24
 * To change this template use File | Settings | File Templates.
 */
case class SearchResultList(data: Seq[Task])

object SearchResultListJsonFormat extends DefaultJsonProtocol {
  implicit val searchResultListFormat = jsonFormat1(SearchResultList)
}