package com.supertaskhelper

import com.typesafe.config.Config
import akka.actor.{ ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension }

import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
class Settings(c: Config) extends Extension {

  val prodAggregationTimeout = c.getMilliseconds("api-sth.task-aggregator.timeout").toLong.milliseconds

  val searchSolr = new SearchSolrSettings(c.getString("api-sth.solr.search"))
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new Settings(system.settings.config)
}

class SearchSolrSettings(config: String) {

  val nap = config
}
