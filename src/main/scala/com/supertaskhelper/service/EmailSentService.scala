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

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 19:16
 * To change this template use File | Settings | File Templates.
 */
trait EmailSentService {
  val collection = MongoFactory.getCollection("emailSent")
  def generateEmailCode(email: String): String = {

    var objectToadd = MongoDBObject(
      "email" -> email,
      "createdDate" -> new Date()
    )
    val id = collection insert objectToadd
    objectToadd._id.get.toString
  }

  def verifyCode(code: String): (Boolean, Int) = {
    try {
      val q = MongoDBObject("_id" -> new org.bson.types.ObjectId(code))

      val res = collection findOne q

      if (res != None) {
        val codeEm = res.get
        val valid = isValidCode(codeEm.getAs[Date]("createdDate").get)
        if (valid)
          (true, 200)
        else
          (false, ERROR_CODE.CODE_EXPIRED)
      } else
        (false, ERROR_CODE.CODE_NOT_EXISTING)
    } catch {
      case _: Throwable => {}
    }
    (false, ERROR_CODE.CODE_NOT_EXISTING)
  }

  private def isValidCode(date: Date): Boolean = {
    val now = new GregorianCalendar();
    now.setTime(date);
    now.add(Calendar.DAY_OF_MONTH, 1);

    !now.getTime().before(new Date());
  }

}

class EmailSentActor(ctx: RequestContext) extends Actor with ActorLogging with EmailSentService {
  val timeout = Duration(context.system.settings.config.getMilliseconds("api-sth.per-request-actor.timeout"), MILLISECONDS)
  context.setReceiveTimeout(timeout)

  def receive = LoggingReceive {

    case c: Code => {
      val res = verifyCode(c.code)
      if (res._1)
        ctx.complete(Response("code verified", 200.toString))
      else
        ctx.complete(Response("code not valid", res._2.toString))

      context.stop(self)
    }

    case _ => {}

  }
}

case class Code(code: String)
