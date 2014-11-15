package com.supertaskhelper.amqp

import akka.actor.{Actor, ActorLogging}
import com.supertaskhelper.common.jms.alerts.IAlert

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 11:50
 * To change this template use File | Settings | File Templates.
 */
object Sender {

  def startSending = {
    // create the connection
    val connection = RabbitMQConnection.getConnection();
    // create the channel we use to send
    val sendingChannel = connection.createChannel();

    val rabbitTemplate = RabbitMQConnection.getRabbitTemplate()
    // make sure the queue exists we want to send to
    //    sendingChannel.queueDeclare(Config.RABBITMQ_QUEUE, false, false, false, null);

  }
}

class SendingAlertsActor() extends Actor with ActorLogging {

  def receive = {

    case alert: IAlert => {

      try {
        val rabbitTemplate = RabbitMQConnection.getRabbitTemplate()
        rabbitTemplate.convertAndSend("alerts", alert)
        log.info(s"MESSAGE SENT alert:${alert} ; killing actor ")
      } catch {
        case e => {
          log.error(s"MESSAGE SENT alert:${alert} ;error :${e} ")
        }
      } finally {
        context.stop(self)
      }

    }
    case message @ _ =>
      log.warning(s"Unknown message received by SendingAlertsActor: ${message}")
  }
}