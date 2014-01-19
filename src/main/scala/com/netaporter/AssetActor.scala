package com.netaporter

import akka.actor.{ Props, Actor }

import com.netaporter.productimage.service.{ ProductCategoryService, ProductAssetAvailabilityService }

import com.netaporter.productimage.domain.{ Assets, Asset }
import com.netaporter.AssetActor.AssetNotFound

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 13/01/2014
 * Time: 11:28
 * To change this template use File | Settings | File Templates.
 */

case class ResourceId(id: Int)
case class ResourceIds(ids: Seq[Int])
case class ResourceIdImages(id: Int, ordered: Boolean)
case class ResourceIdVideos(id: Int)

class AssetActor extends Actor with ProductAssetAvailabilityService with ProductCategoryService {
  private def c = context.system.settings.config
  val rootDirImages = c.getString("digital-assets.dir.root-dir-images")
  val rootDirVideos = c.getString("digital-assets.dir.root-dir-videos")
  val rootDirCacheImages = c.getString("digital-assets.cache-dir.root-dir-images")
  val rootDirCacheVideos = c.getString("digital-assets.cache-dir.root-dir-videos")
  def receive = {
    case msg: ResourceId => {
      //load Images = Videos
      var images = getProductImages(msg.id, rootDirImages)
      var videos = getProductVideos(msg.id, rootDirVideos)
      if (images.size > 0 || videos.size > 0) {
        sender ! Assets(getProductImages(msg.id, rootDirImages).get.map { image => new Asset(msg.id, image, "image", getFullImageUrl(msg.id, rootDirCacheImages)) }
          ::: videos.map { video => new Asset(msg.id, video, "video", rootDirVideos + "/" + video) })
      }

      if (images.size == 0 && videos.size == 0)
        sender ! AssetNotFound
    }

    case msg: ResourceIdImages => {
      var images = if (msg.ordered) getProductImages(msg.id, getCategoryLevel1(msg.id), rootDirImages) else getProductImages(msg.id, rootDirImages)
      sender ! (if (images.size > 0) Assets(images.get.map { image => new Asset(msg.id, image, "image", getFullImageUrl(msg.id, rootDirCacheImages)) })
      else AssetNotFound)
    }

    case msg: ResourceIdVideos => {
      var videos = getProductVideos(msg.id, rootDirVideos);
      sender ! (if (videos.size > 0) Assets(videos.map { video => new Asset(msg.id, video, "video", rootDirCacheVideos + "/" + video) })
      else AssetNotFound)
    }

    case msg: ResourceIds => {
      var assets = msg.ids.map(pid => getProductImages(pid, rootDirImages).get.map { image => new Asset(pid, image, "image", getFullImageUrl(pid, rootDirCacheImages)) }).flatten
      var res = assets ++ msg.ids.map(x => getProductVideos(x, rootDirVideos).map { video => new Asset(x, video, "video", rootDirCacheVideos + "/" + video) }).flatten
      sender ! (if (res.size > 0) Assets(res.toSeq) else AssetNotFound)
    }

  }

}

object AssetActor {
  def props: Props = Props[AssetActor]
  def name = "model"

  case object AssetNotFound

}
