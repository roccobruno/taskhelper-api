package com.supertaskhelper

import com.mongodb.casbah._
import com.mongodb.ServerAddress

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 19/01/2014
 * Time: 17:57
 * To change this template use File | Settings | File Templates.
 */
class MongodbUtil {

}

object MongoFactory {
  private val SERVER = "localhost"
  private val PORT = 27017
  private val DATABASE = "tuttofare"
  private val COLLECTION = "task"

  def getConnection: MongoConnection = return MongoConnection(SERVER)
  def getCollection(conn: MongoConnection): MongoCollection = return conn(DATABASE)(COLLECTION)
  def closeConnection(conn: MongoConnection) { conn.close }

  //  val mongolabUri = System.getProperty("MONGOLAB_URI")
  val mongodbName = System.getProperty("MONGODB_NAME")
  val mongolabUri = "mongodb://taskhelper:taskhelper@ds057528.mongolab.com:57528"

  System.out.println("URI:" + mongolabUri)
  System.out.println("Name:" + mongodbName)

  val uri = if (mongolabUri != null && !mongolabUri.isEmpty) MongoClientURI(mongolabUri) else MongoClientURI("mongodb://localhost:27017")

  val server = new ServerAddress("ds057528.mongolab.com", 57528)
  //  val server = new ServerAddress(SERVER, PORT)
  val credential = MongoCredential("taskhelper", "tuttofare", "taskhelper".toCharArray)
  val mongoClient = MongoClient(server, List(credential))
  //  val mongoClient = MongoClient(server)

  def getDB = if (mongodbName != null && !mongodbName.isEmpty) mongoClient(mongodbName) else mongoClient("tuttofare")

  def getCollection(collName: String) = getDB(collName)

}
