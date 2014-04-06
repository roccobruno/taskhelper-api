package com.supertaskhelper.api

import org.scalatest.{ Matchers, WordSpecLike }
import spray.testkit.ScalatestRouteTest
import concurrent.duration._
import com.supertaskhelper.router.RouteHttpService
import spray.http.StatusCodes
import com.supertaskhelper.domain._

import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.Response
import com.supertaskhelper.service.ConversationMessageService
import com.supertaskhelper.domain.ActivitiesJsonFormat._
import com.supertaskhelper.common.enums.ACTIVITY_TYPE
import java.util.Date


/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecActivities extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)




  def actorRefFactory = system

  "The API Service" should {

     "should return list of the last activities ordered by positionID desc" in new RouteHttpSpecActivities {
       //in the db there are 7 records
       /*
        this is the seventh record

        {
    "_id": {
        "$oid": "533d680ee4b0f9995b88350b"
    },
    "_class": "com.supertaskhelper.common.domain.Activity",
    "subjectId": "53403845036496d20080c279",
    "type": "STH_REGISTRATION",
    "dateCreation": {
        "$date": "2014-04-03T13:54:22.689Z"
    },
    "objectId": "533d67cbe4b0d12a9f1e9568",
    "positionId": 7
}
        */
       Get("/api/activities") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 7)
         assert(responseAs[Activities].activities(0).positionId == 7)
         assert(responseAs[Activities].activities(0).id == "533d680ee4b0f9995b88350b")
         assert(responseAs[Activities].activities(0).subjectId == "53403845036496d20080c279")
         assert(responseAs[Activities].activities(0).activityType == ACTIVITY_TYPE.STH_REGISTRATION.toString)
         assert(responseAs[Activities].activities(0).createdDate.before(new Date()))
         assert(responseAs[Activities].activities(0).objectId.get == "533d67cbe4b0d12a9f1e9568")
         assert(responseAs[Activities].activities(6).positionId == 1)
       }


       Get("/api/activities?subjectId="+"53403845036496d20080c279") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 3)

       }

       Get("/api/activities?subjectId="+"53403845036496d20080c280") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 4)

       }
              //must return the last 2 activities
       Get("/api/activities?positionId="+"6") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 2)
         assert(responseAs[Activities].activities(1).positionId == 6)
         assert(responseAs[Activities].activities(1).activityType == ACTIVITY_TYPE.COMMENTED_ADDED.toString)

       }

       //test pagination
       Get("/api/activities?subjectId="+"53403845036496d20080c280&page=1&sizePage=2") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 2)
         assert(responseAs[Activities].activities(0).positionId == 4)
         assert(responseAs[Activities].activities(1).positionId == 3)
         assert(responseAs[Activities].activities(1).activityType == ACTIVITY_TYPE.COMMENTED_ADDED.toString)
       }

       Get("/api/activities?subjectId="+"53403845036496d20080c280&page=2&sizePage=3") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 1)
       }

       Get("/api/activities?subjectId="+"53403845036496d20082c280") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 0)

       }

       Get("/api/activities?positionId="+"88") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[Activities].activities.size == 0)

       }

     }

  }
}
