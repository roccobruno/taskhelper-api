package com.supertaskhelper.search

import akka.actor._
import akka.event.LoggingReceive
import com.supertaskhelper.DefaultTimeout
import com.supertaskhelper.service.TaskActor
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.search.SearchSolrCoreActor
import akka.actor
import spray.routing.RequestContext
import com.supertaskhelper.search.SearchResultListJsonFormat._
import spray.httpx.SprayJsonSupport._

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

    case SearchParams(terms, objType) => {

      //TODO gestire ricerca per utente and users!!!
      val aggregator = createResultAggregator(self, createTaskFinder)

      createSearchSolrActor tell (terms.split("\\s").toSeq, sender = aggregator)
      //      context.stop(self)
    }

    case s: SearchResultList => {
      httpRequestContext.complete(s)
      context.stop(self)
    }

    case message @ _ =>
      log.warning(s"Unknown message received by SearchActor: ${message}")

  }

  val createSearchSolrActor = context.actorOf(
    Props(new SearchSolrCoreActor {}),
    name = "solr-search-client"
  )

  def createResultAggregator(replyTo: ActorRef, taskActor: ActorRef) = context.actorOf(Props(classOf[TaskAggregator], replyTo, taskActor))
  def createTaskFinder = context.actorOf(Props(classOf[TaskActor]))
}

