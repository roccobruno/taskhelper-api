package com.supertaskhelper

import com.mongodb.casbah._
import com.mongodb.ServerAddress
import com.typesafe.config.ConfigFactory

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
  private val SERVER = ConfigFactory.load().getString("api-sth.database.mongodb-host")
  private val PORT = ConfigFactory.load().getInt("api-sth.database.mongodb-port")
  private val DATABASE = ConfigFactory.load().getString("api-sth.database.mongodb-db")
  private val USERNAME = ConfigFactory.load().getString("api-sth.database.mongodb-username")
  private val PASSWORD = ConfigFactory.load().getString("api-sth.database.mongodb-password")
  private val COLLECTION = "task"

  def getConnection: MongoConnection = return MongoConnection(SERVER)
  def getCollection(conn: MongoConnection): MongoCollection = return conn(DATABASE)(COLLECTION)
  def closeConnection(conn: MongoConnection) { conn.close }

  val server = new ServerAddress(SERVER, PORT)
  val credential = MongoCredential(USERNAME, DATABASE, PASSWORD.toCharArray)
  val mongoClient = MongoClient(server, List(credential))

  def getDB = mongoClient(DATABASE)

  def getCollection(collName: String) = getDB(collName)

}
