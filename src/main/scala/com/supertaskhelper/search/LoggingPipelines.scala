package com.supertaskhelper.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:05
 * To change this template use File | Settings | File Templates.
 */
import akka.actor.{ Actor, ActorLogging }
import Console.{ RED, CYAN, GREEN, RESET }
import spray.http.HttpEntity
import spray.httpx.{ ResponseTransformation, RequestBuilding }

/**
 * Date: 11/03/2013
 * Time: 16:56
 */
trait LoggingPipelines
    extends RequestBuilding
    with ResponseTransformation {
  this: ActorLogging =>

  val colourLogRequest = logRequest(r => {
    log.debug("{}{}{}", CYAN, r, RESET)
  })

  val colourLogResponse = logResponse(r => {
    if (r.status.isFailure) {
      log.error("{}{}{}", RED, r, RESET)
    } else {
      // Empty entity, because otherwise large responses really fill up the logs
      log.debug("{}{}{}", GREEN, r.withEntity(HttpEntity.Empty), RESET)
    }
  })
}
