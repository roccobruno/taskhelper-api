package com.netaporter.productimage

import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import com.netaporter.productimage.service.ProductAssetAvailabilityService

import com.netaporter._


import com.netaporter.productimage.domain.{Assets, Asset}

import com.netaporter.ResourceIdImages
import com.netaporter.ResourceIdVideos
import com.netaporter.ResourceId

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 13/01/2014
 * Time: 14:20
 * To change this template use File | Settings | File Templates.
 */
class AssetActorSpec extends TestKit(ActorSystem()) with FlatSpecLike with ImplicitSender with BeforeAndAfterAll with ProductAssetAvailabilityService {

  override def afterAll() {
    system.shutdown()
  }

  val model = TestActorRef[AssetActor]

  "An actor " should "return 5 images and 1 video file for this pid" in {
    model ! ResourceId(405209)
    val lst = expectMsgType[Assets]
    assert(lst.assets.size === 5)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") ).size === 1)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") )(0).fileName === "405209_detail.flv")
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("image") ).size === 4)
  }




  "An actor " should "return 4 ordered images for this pid" in {
    model ! ResourceIdImages(405209,true)
    val lst = expectMsgType[Assets]
    assert(lst.assets.size === 4)
    assert(lst.assets(0).fileName == "405209_fr_xs.jpg")
    assert(lst.assets(0).fileType == "image")
    assert(lst.assets(1).fileName == "405209_bk_xs.jpg")
    assert(lst.assets(2).fileName == "405209_cu_xs.jpg")
  }

  "An actor" should "return 1 video file for this pid" in {
    model ! ResourceIdVideos(405209)
    val lst = expectMsgType[Assets]
    assert(lst.assets.size === 1)
    assert(lst.assets(0).fileName == "405209_detail.flv")
    assert(lst.assets(0).fileType == "video")
  }

  "An actor" should "return 6 images and 1 video file for those pids" in {
    model ! ResourceIds(Seq(405209,405210))
    val lst = expectMsgType[Assets]
    assert(lst.assets.size === 7)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") ).size === 1)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") )(0).fileName === "405209_detail.flv")
  }

  "An actor" should "return 5 images and 1 video file for those pids, not existing ones" in {
    model ! ResourceIds(Seq(405209,405212))
    val lst = expectMsgType[Assets]
    assert(lst.assets.size === 5)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") ).size === 1)
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("video") )(0).fileName === "405209_detail.flv")
    assert(lst.assets.seq.filter( _.fileType.equalsIgnoreCase("image") ).size === 4)
  }
}
