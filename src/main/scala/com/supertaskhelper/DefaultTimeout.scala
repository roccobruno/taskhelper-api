package com.supertaskhelper

import akka.actor.Actor
import akka.util.Timeout

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 16:51
 * To change this template use File | Settings | File Templates.
 */
trait DefaultTimeout {
  this: Actor =>
  // This timeout is just for resource cleanup.
  // Make sure it is 10% longer than spray's request timeout.
  implicit val timeout = Timeout(context.system.settings.config.getMilliseconds(
    "spray.can.client.request-timeout") * 11 / 10)
}
