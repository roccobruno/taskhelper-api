package com.netaporter.productimage.domain {

  case class Asset(pid: Int, fileName: String, fileType: String, urlFile: String)
  case class Assets(assets: Seq[Asset])

  //   class ImageAsset(pid:Int, fileName:String, rootDir:String) extends Asset(pid,fileName,"image", rootDir+pid)
  //   class VideoAsset(pid:Int, fileName:String, rootDir:String) extends Asset(pid,fileName,"video", rootDir+pid)
}
