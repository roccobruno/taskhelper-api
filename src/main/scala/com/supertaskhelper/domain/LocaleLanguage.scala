package com.supertaskhelper.domain

import java.util.Locale

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/02/2014
 * Time: 17:47
 * To change this template use File | Settings | File Templates.
 */
object LocaleLanguage {

  def getLocaleFromLanguage(lang: String) = lang match {

    case "it" => Locale.ITALIAN
    case "en" => Locale.UK
    case _ => Locale.ITALIAN

  }
}
