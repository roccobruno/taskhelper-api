package com.netaporter.productimage.service

import com.netaporter.productimage.{ FlashVideoFilter, ProductImageFilter }
import java.io.FilenameFilter
import scala.collection.SortedMap

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 09/01/2014
 * Time: 12:19
 * To change this template use File | Settings | File Templates.
 */
trait ProductAssetAvailabilityService {

  //  def getRootDir {
  //    private def c = context.system.settings.config
  //    def interface = c.getString("example-app.service.interface")
  //  }

  //defines the order to use to show images for the Clothing category

  object Clothing extends Enumeration {
    type Clothing = Value
    val in, rw, fr, bk, cu, ou, ou2, ou3, e1, e2, e3, e4, e5, e6, e7, e8 = Value
  }
  //defines the order to use to show images for all the other categories
  object Default extends Enumeration {
    type Default = Value
    val in, rw, ou, fr, bk, cu, ou2, ou3, e1, e2, e3, e4, e5, e6, e7, e8 = Value
  }

  def getEnum(name: String): Enumeration = name match {

    case "Clothing" => Clothing
    case _ => Default
  }

  def getEnumValue(name: String, value: String) = {
    getEnum(name).withName(getMatch(value)).toString
  }

  def getMatch(name: String) = name match {
    case ProductImageFilter.PATTERN_ORDER_MATCH(x) => x
    case _ => ""
  }

  def sortedNames(images: List[String], navLevel1: String): List[String] = {
    getEnum(navLevel1).values.toList
      .flatMap(x => images.filter(getMatch(_) == x.toString))
  }

  /**
   * returns list of product images
   * @param pid   product id
   * @param pngImage   type of image. if true --> png otherwise jog
   * @param hideRunawayImages do not consider runway images
   * @param imageSize size of the images -- default is "xs"
   * @param censorForCountry exclude images for certain country
   * @param navLevel1 classification level 1 --> used to sort the images
   * @param navLevel3 classification level 3. It can be any classification the product belongs to. It used to filter images based on category
   * @return  List of String
   */
  def getProductImages(pid: Int, pngImage: Boolean, hideRunawayImages: Boolean, imageSize: String, censorForCountry: Boolean, navLevel1: String, navLevel3: String, rootDir: String) =
    getImages(pid, pngImage, hideRunawayImages, imageSize, censorForCountry, navLevel1, censorImages(navLevel3), rootDir)

  private def getImages(pid: Int, pngImage: Boolean, hideRunawayImages: Boolean, imageSize: String, censorForCountry: Boolean, navLevel1: String, censorForProduct: Boolean, rootDir: String) = {
    val filter = new ProductImageFilter(pngImage, hideRunawayImages, imageSize, censorForCountry, censorForProduct);
    val res = getAssets(getProductImagesFileLocation(pid, rootDir), filter)

    if (navLevel1.length > 0)
      Option(sortedNames(res, navLevel1))
    else
      Option(res)
  }

  private def getFlashVideos(pid: Int, rootDit: String): List[String] = {

    getAssets(getProductVideoFileLocation(pid, rootDit), new FlashVideoFilter(pid))
  }

  def censorImages(navLevel3: String) = false //TODO add logic to check in the Database the relation classification3 - censor factor

  def getProductImagesFileLocation(pid: Int, productDirsRoot: String) = productDirsRoot + pid
  def getProductVideoFileLocation(pid: Int, productDirsRoot: String) = productDirsRoot + pid;
  /**
   * returns list of file names contained in the <b>rootDir</b> and respecting the rules defined in <b>filter</b>
   * @param rootDir Directory to check
   * @param filter  Filter containing the rule to apply to exclude files from the returned list
   * @return  List of String, each string represents a file name
   */
  def getAssets(rootDir: String, filter: FilenameFilter) = {

    val fileObj = new java.io.File(rootDir)

    if (fileObj.exists() && fileObj.isDirectory) {
      fileObj.list(filter).toList
    } else List()

  }

  /**
   * returns unsorted list of names of all the images associated to the product with the passed <b>pid</b>. All the images are of size "xs"
   * @param pid  product pid
   * @return  List of String, each string represents a file name
   */
  def getProductImages(pid: Int, rootDir: String): Option[List[String]] = getProductImages(pid, false, false, ProductImageFilter.DEFAULT_IMAGE_SIZE, false, "", "", rootDir)

  /**
   * returns sorted list of names of all  all the images associated to the product with the passed <b>pid</b>. All the images are of size "xs"
   * the list is sorted based on the passed <b>navlevel1</b>
   * @param pid product pid
   * @param navlevel1 classification level 1 the product with the passed pid belongs to
   * @return  List of String, each string represents a file name
   */
  def getProductImages(pid: Int, navlevel1: String, rootDir: String): Option[List[String]] = getProductImages(pid, false, false, ProductImageFilter.DEFAULT_IMAGE_SIZE, false, navlevel1, "", rootDir)

  def getProductVideos(pid: Int, rootDir: String): List[String] = getFlashVideos(pid, rootDir)

  def getFullImageUrl(pid: Int, cacheUrl: String) = cacheUrl + "/images/products/" + pid + "/"

}
