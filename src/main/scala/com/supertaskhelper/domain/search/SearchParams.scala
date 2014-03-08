package com.supertaskhelper.domain.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 20:06
 * To change this template use File | Settings | File Templates.
 */
case class SearchParams(terms: String, otype: Option[String], page: Option[Int], sizePage: Option[Int], sort: Option[String], position: Option[String]) {

  require(sort.isEmpty || sort.get == "createdDate desc" || (sort.get == "position" && position.isDefined
    && position.get.contains(",")), "wrong sort value passed. Values accepted are:" +
    "\"createdDate desc\",\"position\". If the choosen value is position, a position value must be passed " +
    "and it must be of the form \"latitude,longitude\"")

}

