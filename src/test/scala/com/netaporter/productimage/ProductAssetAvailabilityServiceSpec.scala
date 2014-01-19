package com.netaporter.productimage

import org.scalatest.FlatSpec
import com.netaporter.productimage.service.ProductAssetAvailabilityService

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 10/01/2014
 * Time: 11:30
 * To change this template use File | Settings | File Templates.
 */
class ProductAssetAvailabilityServiceSpec extends FlatSpec with PropertiesFileReader {



  it should "should return the same list of images files" in new ProductAssetAvailabilityService {
    var list = Seq("405209_bk_xs.jpg","405209_cu_xs.jpg","405209_fr_xs.jpg","405209_ou_xs.jpg")
    assert(list == getProductImages(405209,rootImageDir).get)
  }

  it should "should return the list in the same order" in new ProductAssetAvailabilityService {
    var list = Seq("405209_bk_xs.jpg","405209_cu_xs.jpg","405209_fr_xs.jpg","405209_ou_xs.jpg")
    assert(getProductImages(405209,"Clothing",rootImageDir).get(0) == "405209_fr_xs.jpg")
    assert(getProductImages(405209,"Clothing",rootImageDir).get(1) == "405209_bk_xs.jpg")
    assert(getProductImages(405209,"Clothing",rootImageDir).get(2) == "405209_cu_xs.jpg")
  }

  it should "should return the list in the same order for not Clothing category" in new ProductAssetAvailabilityService {
    var list = Seq("405209_bk_xs.jpg","405209_cu_xs.jpg","405209_fr_xs.jpg","405209_ou_xs.jpg")
    assert(getProductImages(405209,"Shoes",rootImageDir).get(0) == "405209_ou_xs.jpg")
    assert(getProductImages(405209,"Shoes",rootImageDir).get(1) == "405209_fr_xs.jpg")
    assert(getProductImages(405209,"Shoes",rootImageDir).get(2) == "405209_bk_xs.jpg")
  }

}
