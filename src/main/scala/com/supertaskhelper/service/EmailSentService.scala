package com.supertaskhelper.service

import com.supertaskhelper.MongoFactory
import com.mongodb.casbah.commons.MongoDBObject
import java.util.{ Calendar, GregorianCalendar, Date }
import akka.actor.{ Props, ActorContext, Actor, ActorLogging }
import akka.event.LoggingReceive
import com.mongodb.casbah.commons.ValidBSONType.ObjectId
import com.mongodb.casbah.Imports._
import spray.routing.RequestContext
import com.supertaskhelper.domain.Response
import com.supertaskhelper.amqp.SendingAlertsActor
import com.supertaskhelper.router.ERROR_CODE
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.ResponseJsonFormat._
import scala.concurrent.duration._
import com.supertaskhelper.service.Code
import com.supertaskhelper.domain.Response
import spray.routing.RequestContext
import spray.http.StatusCodes

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 19:16
 * To change this template use File | Settings | File Templates.
 */
trait EmailSentService {
  val collection = MongoFactory.getCollection("emailSent")

  def getCodeFromEmail(email:String):Option[String] = {
    val q = MongoDBObject("email" -> email)
    val res = collection findOne q
    res.get.getAs[String]("_id")
  }


  def removeCodeEmail(email:String) {
    val q = MongoDBObject("email" -> email)
    collection remove q
  }


  def generateEmailCode(email: String): String = {

    var objectToadd = MongoDBObject(
      "email" -> email,
      "createdDate" -> new Date()
    )
    val id = collection insert objectToadd
    objectToadd._id.get.toString
  }

  def getEmailSentRecordById(id:String):Option[DBObject] ={
    val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(id))
    val res = collection findOne q
    res
  }



  def verifyCode(codeObj: Option[DBObject]): (Boolean, Int) = {
    var result:(Boolean, Int) = (false, ERROR_CODE.CODE_NOT_EXISTING)
    try {
      if (codeObj != None) {
        val codeEm = codeObj.get
        val valid = isValidCode(codeEm.getAs[Date]("createdDate").get)
        if (valid)
          result = (true, 200)
        else
          result = (false, ERROR_CODE.CODE_EXPIRED)
      } else
        result =  (false, ERROR_CODE.CODE_NOT_EXISTING)
    } catch {
      case _: Throwable => {}
    }
     result
  }

  private def isValidCode(date: Date): Boolean = {
    val now = new GregorianCalendar();
    now.setTime(date);
    now.add(Calendar.DAY_OF_MONTH, 1);

    !now.getTime().before(new Date());
  }

}

class EmailSentActor(ctx: RequestContext) extends Actor with ActorLogging with EmailSentService with UserService {
  val timeout = Duration(context.system.settings.config.getMilliseconds("api-sth.per-request-actor.timeout"), MILLISECONDS)
  context.setReceiveTimeout(timeout)

  def receive = LoggingReceive {

    case c: Code => {
      val codeRecord = getEmailSentRecordById(c.code)
      if(codeRecord.isDefined) {
        val res = verifyCode(codeRecord)
        if (res._1) {
          val email = codeRecord.get.getAs[String]("email").get
          activateAccount(email)
          ctx.complete(Response("code verified", 200.toString))
        }
        else
          ctx.complete(StatusCodes.BadRequest, "Code not valid")
      }else {
        ctx.complete(StatusCodes.BadRequest, "Code not valid")
      }




    }
      context.stop(self)

    case _ => {}

  }
}

case class Code(code: String)
