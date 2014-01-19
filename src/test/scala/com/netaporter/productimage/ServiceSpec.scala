package com.netaporter.productimage
import akka.actor.ActorDSL._
import org.scalatest.{Matchers, FlatSpec}
import spray.testkit.ScalatestRouteTest
import com.netaporter._
import com.netaporter.productimage.domain.{Assets, Asset}
import akka.util.Timeout
import spray.http.StatusCodes
import spray.http.HttpHeaders.`Cache-Control`
import spray.http.CacheDirectives.`max-age`
import scala.concurrent.duration._
import com.netaporter.ResourceId
import spray.http.CacheDirectives.`max-age`
import scala.Some
import spray.httpx.SprayJsonSupport

/**
* Created with IntelliJ IDEA.
* User: r.bruno@london.net-a-porter.com
* Date: 15/01/2014
* Time: 11:19
* To change this template use File | Settings | File Templates.
*/
class ServiceSpec extends FlatSpec with ScalatestRouteTest with SprayJsonSupport with ServiceJsonProtocol with Matchers  {

  import AssetActor._




    val data = for (i <- 0 to 100) yield Asset(i, s"fileName-$i", "image", s"rootdir-$i")
    val summary = (i: Asset) => Assets(Seq(i))

    val model = actor(new Act {
      become {
        case msg: ResourceId => sender ! Assets(Seq(data.find(_.pid == msg.id).get))
        case msg: ResourceIds => sender ! summary
        case msg: ResourceIdImages => sender ! Assets(Seq(data.find(_.pid == msg.id).get))


      }
    })

    implicit def timeout = Timeout(3.second)

    def route = new Service {
      def actorRefFactory = system
    }.route(model)

  import Assets._


    "The Service" should "return a list of 1 item" in {
      Get("/product/2") ~> route ~> check {
        assert(status === StatusCodes.OK)
        assert(header[`Cache-Control`] === Some(`Cache-Control`(`max-age`(0))))
        assert(responseAs[Assets] ===
          Assets(Seq(Asset(2,"fileName-2","image","rootdir-2"))))

      }
    }


  "The Service" should "return a list of 3 item" in {
    Get("/product?ids=1,2,3") ~> route ~> check {
      assert(status === StatusCodes.OK)
      assert(header[`Cache-Control`] === Some(`Cache-Control`(`max-age`(0))))
      assert(responseAs[Assets] ===
        Assets(Seq(Asset(2,"fileName-2","image","rootdir-2" ))))

    }
  }
}
