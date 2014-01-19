package com.netaporter.productimage

import scala.io.Source
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 14/01/2014
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
trait PropertiesFileReader {
  val rootImageDir = ConfigFactory.load().getString("digital-assets.dir.root-dir-images");
  val rootVideosDir = ConfigFactory.load().getString("digital-assets.dir.root-dir-videos");



}
