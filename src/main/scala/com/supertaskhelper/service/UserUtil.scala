package com.supertaskhelper.service

import com.supertaskhelper.common.enums.ACCOUNT_STATUS

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 07/06/2014
 * Time: 11:49
 * To change this template use File | Settings | File Templates.
 */
object  UserUtil extends UserService{

  def isAlreadyUsed(email:String):Boolean = {
    var result = false
    val user = findUserByEmail(email)
    if(user._1) {
      //check teh acc status
      if(user._2.accountStatus.get == ACCOUNT_STATUS.ACTIVE.toString)
       result = true
    }

    result

  }

}
