package com.supertaskhelper

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.MongoConnection
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
  private val DATABASE = "customerDb"
  private val COLLECTION = "customer"

  def getConnection: MongoConnection = return MongoConnection(SERVER)
  def getCollection(conn: MongoConnection): MongoCollection = return conn(DATABASE)(COLLECTION)
  def closeConnection(conn: MongoConnection) { conn.close }
}
