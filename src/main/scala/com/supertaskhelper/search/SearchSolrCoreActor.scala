package com.supertaskhelper.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:03
 * To change this template use File | Settings | File Templates.
 */
import akka.event.LoggingReceive

import spray.httpx.SprayJsonSupport

import com.supertaskhelper.search.SearchSolrCoreActor.{ SearchSolr, SearchResultsSolrWrapper }
import spray.http.{ BasicHttpCredentials, Uri }
import spray.client.pipelining._
import spray.http.MediaTypes._
import akka.pattern.pipe
import spray.can.Http

import akka.event.LoggingReceive
import spray.httpx.SprayJsonSupport

object SearchSolrCoreActor {

  /**
   * DTO
   */
  case class SearchResultsSolrWrapper(response: SearchResults)

  case class SearchResults(numFound: Int, docs: Seq[SolrSearchDoc])

  case class SolrSearchDoc(id: String, otype: Option[String])

  /**
   * Actor Messages
   */
  case class SearchSolr(terms: Seq[String])
}

trait SearchSolrCoreActor
    extends HttpClient
    with LoggingPipelines
    with SprayJsonSupport
    with ClientDirectives

    with SolrSearchJsonProtocol {

  lazy val pipeline = (
    colourLogRequest

    ~> addCredentials(BasicHttpCredentials("sthsolr", "sthrocluigiosolr3"))
    ~> sendReceive(transport)
    ~> colourLogResponse
    ~> forceMediaType(`application/json`)
    ~> unmarshal[SearchResultsSolrWrapper]
  )

  def receive = LoggingReceive {

    case terms: Seq[String] =>

      val sanitisedTerms = terms.map(sanitiseTerm)

      val termStatement = sanitisedTerms match {
        case term :: Nil => term
        case _ =>
          val termsSpace = terms.mkString(" ")
          val termsAnd = terms.mkString(" AND ")
          s"(($termsSpace) OR ($termsAnd))"
      }

      val uri = Uri(settings.searchSolr.nap + "/select") withQuery (
        "q" -> s"$termStatement",
        "fl" -> "id,type",
        //        "qf" -> boostStatement(locale.lang),
        "sort" -> "score desc",
        "q.op" -> "AND",
        "wt" -> "json",
        "defType" -> "edismax"
      )

      val f = pipeline(Get(uri))
        .map(_.response)

      f pipeTo sender
  }

  /**
   * Makes a SOLR query like:
   *  AND (field:1 OR field:2 OR field:3)
   */
  def orStatement(field: String, values: Seq[Any]) =
    if (values.isEmpty) ""
    else values.mkString(s" AND ($field:", s" OR $field:", ")")

  /**
   * See
   *  - http://lucene.apache.org/core/3_6_0/queryparsersyntax.html#Escaping%20Special%20Characters
   *  - http://javahacker.com/abusing-the-solr-local-parameters-feature-localparams-injection/
   *
   * SOLR seems to already escape backslashes
   * We consider these chars valid in user search terms and are therefore not escaped: + - " * ?
   * Anything that needs to know the SOLR field name should be escaped
   */
  val charsToEscape = Set(
    "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "~", ":"
  )

  def sanitiseTerm(term: String) =
    charsToEscape.foldLeft(term) {
      (t, ch) => t.replace(ch, "\\" + ch)
    }
}
