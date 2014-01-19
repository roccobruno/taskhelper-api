package com.netaporter.productimage

import java.io.{ File, FilenameFilter }

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 14/01/2014
 * Time: 10:35
 * To change this template use File | Settings | File Templates.
 */
class FlashVideoFilter(pid: Int) extends FilenameFilter {

  def accept(p1: File, p2: String): Boolean = p2 == (pid + "_detail.flv")
}
