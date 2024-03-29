package com.supertaskhelper.search

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 18:03
 * To change this template use File | Settings | File Templates.
 */
import akka.event.LoggingReceive
import akka.pattern.pipe
import com.supertaskhelper.Settings
import com.supertaskhelper.domain.search.SearchParams
import com.supertaskhelper.search.SearchSolrCoreActor.SearchResultsSolrWrapper
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{BasicHttpCredentials, Uri}
import spray.httpx.SprayJsonSupport
import spray.json._

object SearchSolrCoreActor {

  /**
   * DTO
   */

  case class SolrSearchDoc(id: String, otype: Option[String], distance: Option[String])

  object SolrSearchDocFormat extends DefaultJsonProtocol {
    implicit object SolrSearchDocJsonFormat extends RootJsonFormat[SolrSearchDoc] {
      def write(c: SolrSearchDoc) = JsObject(
        "id" -> JsString(c.id),
        "type" -> JsString(c.otype.get)

      )
      def read(value: JsValue) = {
        value.asJsObject.getFields("id", "type", "_dist_") match {
          case Seq(JsString(id), JsString(otype), JsNumber(_dist_)) =>
            new SolrSearchDoc(id, Option(otype), Option(_dist_.toString))
          case _ => throw new DeserializationException("Color expected")
        }
      }
    }

  }

  case class SearchResults(numFound: Int, docs: Seq[SolrSearchDoc])

  case class SearchResultsSolrWrapper(response: SearchResults)

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

    with SearchJsonProtocol {

  lazy val pipeline = (
    colourLogRequest

    ~> addCredentials(BasicHttpCredentials(Settings.solr_username, Settings.solr_password))
    ~> sendReceive(transport)
    ~> colourLogResponse
    ~> forceMediaType(`application/json`)
    ~> unmarshal[SearchResultsSolrWrapper]
  )

  def receive = LoggingReceive {
    //    terms.split("\\s").toSeq
    case s: SearchParams =>

      val uri = Uri(settings.searchSolr.nap + "/select") withQuery (buildQuery(s))
      log.info("solr request:{}", uri)
      val f = pipeline(Get(uri))
        .map(_.response)

      f pipeTo sender
  }

  private def buildQuery(s: SearchParams): Map[String, String] = {
    val sanitisedTerms = orStatement("search_field", s.terms.getOrElse("").split("\\s").toSeq.map(sanitiseTerm))
    val tesrmsWitType = s.otype match {
      case None => sanitisedTerms
      case _ => {
        val typeClause = sanitisedTerms + " AND type:" + s.otype.get.toUpperCase
        typeClause
      }

    }
    if (s.sort.getOrElse("createdDate desc") contains ("position")) {
      Map(
        "q" -> s"$tesrmsWitType",
        "sort" -> "geodist() asc",
        "q.op" -> "AND",
        "wt" -> "json",
        "start" -> getPage(s.page, s.sizePage),
        "rows" -> s.sizePage.getOrElse(10).toString,
        "defType" -> "edismax",
        "fl" -> "_dist_:geodist(),type,id",
        "pt" -> s.position.get,
        "sfield" -> "position"
      )
    } else {
      Map(
        "q" -> s"$tesrmsWitType",
        "fl" -> "score,type,id,_dist_:score",
        "sort" -> s.sort.getOrElse("score desc"),
        "q.op" -> "AND",
        "wt" -> "json",
        "start" -> getPage(s.page, s.sizePage),
        "rows" -> s.sizePage.getOrElse(10).toString,
        "defType" -> "edismax"

      )
    }
  }

  private def getPage(page: Option[Int], sizePage: Option[Int]): String = {
    if (page.isDefined)
      (((page.get - 1) * sizePage.getOrElse(10))).toString
    else {
      0.toString
    }
  }

  /**
   * Makes a SOLR query like:
   *  AND (field:1 OR field:2 OR field:3)
   */
  def orStatement(field: String, values: Seq[Any]) =
    if (values.isEmpty || (values.seq.size == 1 && values.seq(0) == "")) "*:*"
    else values.mkString(s" ($field:", s" OR $field:", ")")

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
