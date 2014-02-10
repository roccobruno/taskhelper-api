package com.supertaskhelper.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:49
 * To change this template use File | Settings | File Templates.
 */
import spray.json.DefaultJsonProtocol

/**
 * Converts fields from camelCase to snake_case
 * For use when querying SOLR
 */
trait SnakifiedSprayJsonSupport extends DefaultJsonProtocol {
  import scala.reflect._

  //Don't snakify the fields in the blacklist
  def blacklist: Set[String]

  override protected def extractFieldNames(classTag: ClassTag[_]) = {
    def snakify(name: String) = CASE_CHANGE.replaceAllIn(name, REPLACEMENT).toLowerCase

    super.extractFieldNames(classTag).map {
      f => if (blacklist.contains(f)) f else snakify(f)
    }
  }

  private val CASE_CHANGE = """([a-z])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"
}
