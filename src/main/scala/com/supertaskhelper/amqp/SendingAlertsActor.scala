package com.supertaskhelper.amqp

import akka.actor.{ ActorLogging, Actor }
import com.rabbitmq.client.Channel
import scala.util.parsing.json.JSON
import spray.routing.RequestContext
import com.supertaskhelper.common.jms.alerts.CreatedTaskAlert

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

    case alert: CreatedTaskAlert => {
      RabbitMQConnection.getRabbitTemplate().convertAndSend("alerts", alert)
      context.stop(self)
    }
    case _ => {}
  }
}