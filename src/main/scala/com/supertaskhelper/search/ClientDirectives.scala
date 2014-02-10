package com.supertaskhelper.search

import spray.http.{ ContentType, HttpEntity, MediaType }
import spray.http.HttpEntity.NonEmpty
import spray.httpx.ResponseTransformation._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/02/2014
 * Time: 20:28
 * To change this template use File | Settings | File Templates.
 */
trait ClientDirectives {

  /*
    This is required only because SOLR is lying about its Content-Type when
    you request JSON. Instead it returns `plain/text`.
   */
  def forceMediaType(m: MediaType): ResponseTransformer =
    r => r.mapEntity {
      case NonEmpty(_, data) => HttpEntity(ContentType(m), data)
      case x => x
    }

}

object ClientDirectives extends ClientDirectives