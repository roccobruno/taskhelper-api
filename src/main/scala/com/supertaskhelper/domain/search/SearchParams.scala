package com.supertaskhelper.domain.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 20:06
 * To change this template use File | Settings | File Templates.
 */
case class SearchParams(terms: Option[String], otype: Option[String], page: Option[Int], sizePage: Option[Int], sort: Option[String], position: Option[String]) {

  require(sort.isEmpty || (sort.get == "createdDate desc" && otype.isDefined && otype.get == "TASK") || (sort.get == "position" && position.isDefined
    && position.get.contains(",")), "wrong sort value passed. Values accepted are:" +
    "\"createdDate desc (only for TASKS)\",\"position\". If the choosen value is position, a position value must be passed " +
    "and it must be of the form \"latitude,longitude\"")

  require(otype.isDefined && (otype.get == "USER" || otype.get == "TASK"), "otype cannot be empty. It must be either of value USER or TASK")
  require(page.isEmpty || page.get > 0, "page value must be greater than 0")
  require(sizePage.isEmpty || sizePage.get >= 0, "sizePage value must be greater or equals to 0")

}

