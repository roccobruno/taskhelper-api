package com.supertaskhelper.search

import akka.actor.{ ReceiveTimeout, ActorLogging, Actor, ActorRef }
import java.util.Locale
import com.supertaskhelper.Settings
import com.supertaskhelper.search.TaskAggregator.{ NotEnriched, Enriched, Enrichable }
import akka.event.LoggingReceive
import com.supertaskhelper.domain.Task
import com.supertaskhelper.service.TaskNotFound
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResults
import com.supertaskhelper.service.TaskServiceActor.FindTask
import org.bson.types.ObjectId
import spray.json.DefaultJsonProtocol

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 16:58
 * To change this template use File | Settings | File Templates.
 */
class TaskAggregator(replyTo: ActorRef, taskActorFinder: ActorRef)
    extends Actor
    with ActorLogging {

  import context._

  val settings = Settings(system)

  // This actor is scoped to the request. If aggregation takes too long, we must kill ourselves
  setReceiveTimeout(settings.prodAggregationTimeout)

  // This contains a list of SolrSearchDocs
  // - some are waiting to be enriched with information from the PS API (NotEnriched)
  // - some have been enriched with information from the PS API (Enriched)
  var result = Seq.empty[Enrichable]

  def receive = LoggingReceive {

    case ReceiveTimeout =>
      log.error("Timed out whilst enriching tasks {}", result)
      stop(self)

    case TaskNotFound(_) =>
      stop(self)

    case SearchResults(_, docs) =>
      if (docs.isEmpty) replyTo ! SearchResultList(Nil)

      // We have received a list of search results for the SOLR search core
      result = docs.map(doc => NotEnriched(new ObjectId(doc.id)))

      // Let's fire a request to the PS API for each task
      docs.foreach(doc => taskActorFinder ! FindTask(doc.id))

    case p: Task =>
      // We have received a full task back from the TaskFinder
      // Let's replace any non-enriched tasks (BasicTask) for this pid and channel ID with this FullTask
      val enrich = result.map {
        case NotEnriched(pid) if pid == p.id.get => Enriched(p)
        case x => x
      }

      //If enriching had no affect, then we must have received a incorrect task
      if (result == enrich) {
        log.error("Received unexpected task {} whilst enriching {}", p, result)
        stop(self)
      }

      result = enrich

      // After every enrichment, check if we are finished
      checkForFinished
  }

  override def unhandled(x: Any) = {
    stop(self)
    log.error("Stopping due to receiving unhandled message {}", x)
  }

  /**
   * Send the search results we have enriched with task information so far
   * Forget about the search results we haven't enriched
   */
  def sendResponse = {
    replyTo ! SearchResultList(enrichedSoFar)
    stop(self)
  }

  /**
   * Check if we have enriched all the search results with task information from the PS API
   */
  def checkForFinished =
    if (enrichedSoFar.size == result.size) sendResponse

  /**
   * We send an individual request to the PS API for each task search result.
   * This method returns the search results that we have enriched with PS API responses
   */
  def enrichedSoFar = result.collect {
    case Enriched(x) => x
  }
}

object TaskAggregator {

  trait Enrichable {
    def toTask: Task
  }
  case class NotEnriched(taskId: ObjectId) extends Enrichable {
    def toTask = throw new NoSuchElementException("Cannot covert search result to task as it not been enriched with data from PS API")
  }
  case class Enriched(prod: Task) extends Enrichable {
    def toTask = prod
  }
}

