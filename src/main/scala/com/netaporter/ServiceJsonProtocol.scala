package com.netaporter

import spray.json._
import com.netaporter.productimage.domain.{ Assets, Asset }

trait ServiceJsonProtocol extends DefaultJsonProtocol {

  implicit val assetFmt = jsonFormat4(Asset.apply)
  implicit val assetsFmt = jsonFormat1(Assets.apply)

}
