package com.supertaskhelper.service

import com.supertaskhelper.MongoFactory

trait MongodbService {

  val conn = MongoFactory.getConnection

}
