package com.supertaskhelper.util

import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.domain.Address

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 05/04/2014
 * Time: 17:41
 * To change this template use File | Settings | File Templates.
 */
trait ConverterUtil {

  def getMongoDBObjFromAddress(address: Address) = {
    MongoDBObject(
      "city" -> address.city,
      "country" -> address.country,
      "postcode" -> address.postcode,
      "regione" -> address.regione,
      "address" -> address.address,
      "location" -> MongoDBObject(
        "latitude" -> address.location.get.latitude,
        "longitude" -> address.location.get.longitude
      )
    )
  }

}
