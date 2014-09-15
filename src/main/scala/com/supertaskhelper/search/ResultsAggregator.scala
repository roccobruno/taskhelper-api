package com.supertaskhelper.search

import akka.actor._
import java.util.Locale
import com.supertaskhelper.Settings

import akka.event.LoggingReceive
import com.supertaskhelper.domain.{ User, Tasks, TaskParams, Task }
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResults
import com.supertaskhelper.service.TaskServiceActor.FindTask
import org.bson.types.ObjectId
import spray.json.DefaultJsonProtocol
import com.supertaskhelper.domain.search.Searchable
import com.supertaskhelper.search.ResultsAggregator.{ Enrichable, NotEnriched, Enriched }
import com.supertaskhelper.service.actors._
import com.supertaskhelper.search.ResultsAggregator.NotEnriched
import com.supertaskhelper.domain.TaskParams
import com.supertaskhelper.domain.User
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Tasks
import com.supertaskhelper.search.ResultsAggregator.Enriched
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResults
import com.supertaskhelper.search.SearchResultList
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.search.ResultsAggregator.NotEnriched
import com.supertaskhelper.domain.TaskParams
import com.supertaskhelper.domain.User
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Tasks
import com.supertaskhelper.search.ResultsAggregator.Enriched
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResults
import com.supertaskhelper.search.SearchResultList
import com.supertaskhelper.service.TaskServiceActor.FindTask
import com.supertaskhelper.service.actors.TaskNotFound
import com.supertaskhelper.search.ResultsAggregator.NotEnriched
import com.supertaskhelper.domain.TaskParams
import com.supertaskhelper.service.actors.UserNotFound
import com.supertaskhelper.domain.User
import com.supertaskhelper.service.actors.FindUser
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Tasks
import com.supertaskhelper.search.ResultsAggregator.Enriched
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResults
import com.supertaskhelper.search.SearchResultList
import com.supertaskhelper.service.TaskServiceActor.FindTask

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 16:58
 * To change this template use File | Settings | File Templates.
 */
class ResultsAggregator(replyTo: ActorRef, taskActorFinder: ActorRef, userTaskFinder: ActorRef)
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

    case UserNotFound(_) =>
      stop(self)

    case TaskNotFound(_) =>
      stop(self)

    case SearchResults(_, docs) => {
      if (docs.isEmpty) replyTo ! SearchResultList(Nil, Nil)

      // We have received a list of search results for the SOLR search core
      result = docs.map(doc => NotEnriched(new ObjectId(doc.id)))

      // Let's fire a request to the PS API for each task
      docs.foreach(doc =>
        (if (doc.otype.getOrElse("TASK") == "TASK") { createTA ! FindTask(TaskParams(Option(doc.id), None, None, None, None, None, None, None, doc.distance)) }
        else { createUS ! FindUser(doc.id, doc.distance) }
        ))
    }
    case p: Tasks => {
      log.info(p.toString)
      // We have received a full task back from the TaskFinder
      // Let's replace any non-enriched tasks (BasicTask) for this pid and channel ID with this FullTask
      for (x <- p.tasks) {
        val enrich = result.map {
          case NotEnriched(pid) if pid == x.id.get => Enriched(x)
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

    }

    case u: User =>
      // We have received a full task back from the TaskFinder
      // Let's replace any non-enriched tasks (BasicTask) for this pid and channel ID with this FullTask
      {
        val enrich = result.map {
          case NotEnriched(pid) if pid == u.id => Enriched(u)
          case x => x
        }

        //If enriching had no affect, then we must have received a incorrect task
        if (result == enrich) {
          log.error("Received unexpected user {} whilst enriching {}", u, result)
          stop(self)
        }

        result = enrich

        // After every enrichment, check if we are finished
        checkForFinished
      }

    case message @ _ => {
      log.warning(s"Unknown message received by SearchActor: ${message}")
      context.stop(self)
    }
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
    replyTo ! SearchResultList(taskEnrichedSoFar, userEnrichedSoFar)
    stop(self)
  }

  /**
   * Check if we have enriched all the search results with task information from the PS API
   */
  def checkForFinished =
    if ((taskEnrichedSoFar.size + userEnrichedSoFar.size) == result.size) sendResponse

  /**
   * We send an individual request to the PS API for each task search result.
   * This method returns the search results that we have enriched with PS API responses
   */
  def taskEnrichedSoFar = result.collect {
    case Enriched(x) if x.isInstanceOf[Task] => x
  }

  def userEnrichedSoFar = result.collect {
    case Enriched(x) if x.isInstanceOf[User] => x
  }

  def createTA(): ActorRef = {
    context.actorOf(Props(classOf[TaskActor]))
  }

  def createUS(): ActorRef = {
    context.actorOf(Props(classOf[UserActor]))
  }

}

object ResultsAggregator {

  trait Enrichable {
    def toTask: Searchable

  }

  trait UserEnrichable {
    def toUser: User
  }
  case class NotEnriched(taskId: ObjectId) extends Enrichable {
    def toTask = throw new NoSuchElementException("Cannot covert search result to task as it not been enriched with data from PS API")
  }
  case class Enriched(prod: Searchable) extends Enrichable {
    def toTask = prod
  }

}

