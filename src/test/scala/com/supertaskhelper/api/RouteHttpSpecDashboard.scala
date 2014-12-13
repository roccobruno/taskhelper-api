package com.supertaskhelper.api

import com.supertaskhelper.domain.Dashboard
import com.supertaskhelper.domain.DashboardJsonFormat._
import com.supertaskhelper.router.RouteHttpService
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._


/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecDashboard extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(25 seconds)




  def actorRefFactory = system

  "The API Service" should {


    val payId = "PAY-1FX913169V0229117KRTXFIY"
      val userId="53028f49036462126f7f042b"

     "return dashboard with id" in {
       Get("/api/dashboard?userId="+userId) ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Dashboard].taskAsTP.assigned == 7)
           assert(responseAs[Dashboard].taskAsTP.completed == 1)
           assert(responseAs[Dashboard].taskAsTP.open ==1)
           assert(responseAs[Dashboard].taskAsTP.closed ==3)
           assert(responseAs[Dashboard].taskAsTP.requested ==9)
           assert(responseAs[Dashboard].taskAsTP.waitingReview ==1)



           assert(responseAs[Dashboard].taskAsSTH.assigned ==1)
           assert(responseAs[Dashboard].taskAsSTH.closed ==1)
           assert(responseAs[Dashboard].taskAsSTH.completed ==1)
           assert(responseAs[Dashboard].taskAsSTH.open ==1)
           assert(responseAs[Dashboard].taskAsSTH.requests ==1)



           assert(responseAs[Dashboard].sthMoney.toWithdraw == BigDecimal("22"))
           assert(responseAs[Dashboard].tpMoney.alreadyPaid == BigDecimal("26"))






       }
     }





  }
}
