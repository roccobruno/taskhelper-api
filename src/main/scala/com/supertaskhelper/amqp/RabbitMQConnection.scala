package com.supertaskhelper.amqp

import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.AMQP.Connection

import com.rabbitmq.client
import com.rabbitmq.client.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 20/01/2014
 * Time: 09:20
 * To change this template use File | Settings | File Templates.
 */

object Config {
  val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
  val RABBITMQ_QUEUE = ConfigFactory.load().getString("rabbitmq.queue");
  val RABBITMQ_EXCHANGEE = ConfigFactory.load().getString("rabbitmq.exchange");
}

object RabbitMQConnection {

  private val connection: client.Connection = null
  private var template: RabbitTemplate = null

  /**
   * Return a connection if one doesn't exist. Else create
   * a new one
   */
  def getConnection(): client.Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory();
        factory.setHost(Config.RABBITMQ_HOST);
        factory.newConnection();
      }
      case _ => connection
    }
  }

  def getRabbitTemplate(): RabbitTemplate = {
    template match {
      case null => {
        var uri = System.getProperty("CLOUDAMQP_URL_EXT");
        if (uri == null) uri = "amqp://guest:guest@localhost";
        var rc = new com.rabbitmq.client.ConnectionFactory();
        rc.setUri(uri);
        val cf = new CachingConnectionFactory(rc);
        template = new RabbitTemplate(cf)
        template
      }
      case _ => template

    }

  }

}
