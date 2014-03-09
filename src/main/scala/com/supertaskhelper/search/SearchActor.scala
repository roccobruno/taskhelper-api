package com.supertaskhelper.search
import spray.caching.{ LruCache, Cache }

import spray.util._
import scala.concurrent.Future

import akka.actor._
import akka.event.LoggingReceive
import com.supertaskhelper.DefaultTimeout
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.search.SearchSolrCoreActor
import akka.actor
import spray.routing.RequestContext
import com.supertaskhelper.search.SearchResultListJsonFormat._
import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.service.actors.{ UserActor, TaskActor }

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
class SearchActor(httpRequestContext: RequestContext) extends Actor with ActorLogging with DefaultTimeout {
  import context.system

  def receive = LoggingReceive {

    case s: SearchParams => {

      val aggregator = createResultAggregator(self, createTaskFinder, createUserActorFinder)
      createSearchSolrActor tell (s, sender = aggregator)

    }

    case s: SearchResultList => {
      httpRequestContext.complete(s)
      context.stop(self)
    }

    case message @ _ => {
      log.warning(s"Unknown message received by SearchActor: ${message}")
      context.stop(self)
    }

  }

  val createSearchSolrActor = context.actorOf(
    Props(new SearchSolrCoreActor {}),
    name = "solr-search-client"
  )

  def createResultAggregator(replyTo: ActorRef, taskActor: ActorRef, userActor: ActorRef) = context.actorOf(Props(classOf[ResultsAggregator], replyTo, taskActor, userActor))
  def createTaskFinder = context.actorOf(Props(classOf[TaskActor]))
  def createUserActorFinder = context.actorOf(Props(classOf[UserActor]))
}

