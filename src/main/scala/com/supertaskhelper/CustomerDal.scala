package com.supertaskhelper

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject

case class Customer(firstName: Some[Any], lastName: Some[Any], _id: Some[Any], phoneNumber: Some[Any], address: Some[Any],
  city: Some[Any], country: Some[Any],
  zipcode: Some[Any])

class CustomerDal {

  val conn = MongoFactory.getConnection

  def saveCustomer(customer: Customer) = {
    val customerObj = buildMongoDbObject(customer)
    val result = MongoFactory.getCollection(conn).save(customerObj)
    val id = customerObj.get("_id")
    println(id)
    id
  }

  def findCustomer(id: String) = {
    var q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))
    val collection = MongoFactory.getCollection(conn)
    val result = collection findOne q

    val customerResult = result.get

    val customer = Customer(Some(customerResult.get("firstName")),
      lastName = Some(customerResult.get("lastName")),
      _id = Some(customerResult.get("_id").toString()),
      phoneNumber = Some(customerResult.get("phoneNumber")),
      address = Some(customerResult.get("address")),
      city = Some(customerResult.get("city")),
      country = Some(customerResult.get("country")),
      zipcode = Some(customerResult.get("zipcode")))

    customer //return the customer object
  }

  //Convert our Customer object into a BSON format that MongoDb can store.
  private def buildMongoDbObject(customer: Customer): DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "firstName" -> customer.firstName
    builder += "lastName" -> customer.lastName
    builder += "phoneNumber" -> customer.phoneNumber.getOrElse("")
    builder += "address" -> customer.address.getOrElse("")
    builder += "city" -> customer.city.get
    builder += "country" -> customer.country.get
    builder += "zipcode" -> customer.zipcode.getOrElse("")
    builder.result()
  }
}