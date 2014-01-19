package com.netaporter.productimage

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 08/01/2014
 * Time: 16:30
 * To change this template use File | Settings | File Templates.
 */

object ProductImageFilter {

  val PNG_EXTENSION = "PNG"
  val JPG_EXTENSION = "JPG"

  val DEFAULT_IMAGE_SIZE = "xs"

  val RUNWAY_INDICATOR = "_rw_"
  val MODEL_INDICATOR = "_in_"
  val REVERSE_AUCTION_INDICATOR = "_revauc"
  val REVERSE_AUCTION_SIZE_INDICATOR = "revauc"

  val PATTERN_ORDER_MATCH = "[^_]*_(\\w{2,3})_.*".r
}

import java.io.FilenameFilter

class ProductImageFilter(pngImage: Boolean, hideRunwaysImages: Boolean, imageSize: String, censorForCountry: Boolean, censorForProduct: Boolean) extends FilenameFilter {

  def accept(dif: java.io.File, fileName: String) = (isCorrectSize(fileName) && isCorrectExtension(fileName) && isModelAcceptable(fileName) && isRunwayAcceptable(fileName) && isReverseAuctionAcceptable(fileName))

  // Checks that the file name indicates that the image is the correct size
  def isCorrectSize(g: String) = g.toLowerCase.contains("_" + imageSize.toLowerCase + ".")

  // Determines the appropriate file extension for all images
  def imageExtension = if (pngImage) ProductImageFilter.PNG_EXTENSION else ProductImageFilter.JPG_EXTENSION

  // Determines whether or not model images should be displayed given the context
  def modelCheckRequired = censorForCountry && censorForProduct

  // Checks that the file name indicates that it the image is the correct type
  def isCorrectExtension(g: String) = g.toLowerCase().contains("." + imageExtension toLowerCase ())

  // Checks that if model images should not be displayed that this is not a model image
  def isModelAcceptable(g: String) = !(modelCheckRequired && !(g.toLowerCase.contains(ProductImageFilter.MODEL_INDICATOR)))

  // Checks that if runway images should not be displayed that this is not a runway image
  def isRunwayAcceptable(g: String) = !(hideRunwaysImages && g.toLowerCase.contains(ProductImageFilter.RUNWAY_INDICATOR))

  // Check if reverse auction images should be displayed
  // this returns true if image is not reverse auction
  // if image is a reverse auction image and image size contains string 'revauc' then it returns true
  //for example : to display a revauc image, pass imagesize as 'revauc_l'
  def isReverseAuctionAcceptable(fileName: String) = !(fileName.contains(ProductImageFilter.REVERSE_AUCTION_INDICATOR) && !(imageSize.contains(ProductImageFilter.REVERSE_AUCTION_SIZE_INDICATOR)))

}
