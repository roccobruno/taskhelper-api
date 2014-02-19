package com.supertaskhelper.service

import spray.http.HttpHeaders.`Cache-Control`
import spray.http.CacheDirectives.`max-age`

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 17/02/2014
 * Time: 20:24
 * To change this template use File | Settings | File Templates.
 */
trait Service {

  val CacheHeader = (maxAge: Long) => `Cache-Control`(`max-age`(maxAge)) :: Nil

  val MaxAge404 = 600l

}
