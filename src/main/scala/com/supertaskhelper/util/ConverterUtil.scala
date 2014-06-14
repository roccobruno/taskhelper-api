package com.supertaskhelper.util

import com.mongodb.casbah.commons.MongoDBObject
import com.supertaskhelper.domain.{ Task, Address }

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

  def getMongoDBObjFromTaskPrice(task: Task) = {
    MongoDBObject("numOfWorkingHour" ->
      (if (task.taskPrice.isDefined) task.taskPrice.get.nOfHours.getOrElse(0) else "0"),
      "tariffWithoutFeeForSTH" -> (if (task.taskPrice.isDefined) task.taskPrice.get.tariffWithoutFeeForSth.getOrElse("0") else "0"),
      "tariffWithFeeForSTH" -> (if (task.taskPrice.isDefined) task.taskPrice.get.tariffWithFeeForSth.getOrElse("0") else "0"))

  }

}
