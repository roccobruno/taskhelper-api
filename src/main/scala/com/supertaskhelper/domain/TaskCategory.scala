package com.supertaskhelper.domain

import spray.json.DefaultJsonProtocol

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 10:39
 * To change this template use File | Settings | File Templates.
 */
case class TaskCategories(categories: Seq[TaskCategory])

case class TaskCategory(id: String, categoryType: Option[String], title_it: Option[String],
  title_en: Option[String], description: Option[String], order: Option[Int], tariff: Option[String]) //tariff used in case of userskill

object TaskCategoryJsonFormat extends DefaultJsonProtocol {
  implicit val taskCategoryFormat = jsonFormat7(TaskCategory)
}

object TaskCategoriesJsonFormat extends DefaultJsonProtocol {
  implicit val taskCategoryFormat = jsonFormat7(TaskCategory)
  implicit val taskCategoriesFormat = jsonFormat1(TaskCategories)
}
