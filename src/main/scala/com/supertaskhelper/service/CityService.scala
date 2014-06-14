package com.supertaskhelper.service

import com.supertaskhelper.util.ConverterUtil
import com.supertaskhelper.domain.Address
import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.MongoFactory

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 14/06/2014
 * Time: 16:42
 * To change this template use File | Settings | File Templates.
 */
trait CityService extends Service with ConverterUtil {

  def saveCityFromAddress(address: Address, language: String) {

    val obj = MongoDBObject(

      "country" -> address.country,
      "location" -> MongoDBObject(
        "longitude" -> address.location.get.longitude,
        "latitude" -> address.location.get.latitude

      ),

      "regioneEn" -> (if ("en".equalsIgnoreCase(language)) address.regione.getOrElse("") else ""),
      "regioneIt" -> (if ("it".equalsIgnoreCase(language)) address.regione.getOrElse("") else ""),

      "nameEn" -> (if ("en".equalsIgnoreCase(language)) address.city.getOrElse("") else ""),
      "nameIt" -> (if ("it".equalsIgnoreCase(language)) address.city.getOrElse("") else "")
    )

    val collection = MongoFactory.getCollection("city")
    collection save obj
  }

}
