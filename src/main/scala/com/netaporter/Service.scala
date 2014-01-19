package com.netaporter

import spray.routing.{ HttpServiceActor, ExceptionHandler, HttpService }
import akka.actor.{ Props, Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.`Cache-Control`
import spray.http.StatusCodes._
import com.netaporter.AssetActor.{ AssetNotFound }
import com.netaporter.productimage.domain.{ Assets, Asset }
import spray.util.LoggingContext

object ServiceActor {
  def props(model: ActorRef)(implicit timeout: Timeout): Props = Props(classOf[ServiceActor], model, timeout)
  def name = "service"
}

class ServiceActor(model: ActorRef, implicit val askTimeout: Timeout) extends HttpServiceActor with Service {
  override def actorRefFactory = context
  private def c = context.system.settings.config
  val cacheHeaderDuration = c.getInt("digital-assets.cache-duration")
  def receive = runRoute(route(model))
  override def cacheDuration = cacheHeaderDuration
}

trait Service extends HttpService with ServiceJsonProtocol {

  def cacheDuration = 0

  import scala.language.postfixOps // for 'q ? in parameter() below

  implicit def ec = actorRefFactory.dispatcher

  val CacheHeader = (maxAge: Long) => `Cache-Control`(`max-age`(maxAge)) :: Nil

  val MaxAge404 = 600l

  def route(model: ActorRef)(implicit askTimeout: Timeout) =
    get {
      //get all the assets for the product with id
      path("product" / IntNumber) { id =>
        onSuccess(model ? ResourceId(id)) {
          case assets: Assets =>
            complete(OK, CacheHeader(cacheDuration), assets)

          case AssetNotFound =>
            complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
        }
      } ~
        //get all the images for product with id
        path("product" / "images" / IntNumber) {
          id =>
            {
              parameter('ordered ?) { term =>
                import java.lang.{ Boolean => JBoolean };
                var msg = ResourceIdImages(id, JBoolean.valueOf(term.get))
                onSuccess(model ? msg) {
                  case assets: Assets =>
                    complete(OK, CacheHeader(cacheDuration), assets)

                  case AssetNotFound =>
                    complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
                }
              }
            }
        } ~
        //get all the videos
        path("product" / "videos" / IntNumber) {
          id =>
            {

              var msg = ResourceIdVideos(id)
              onSuccess(model ? msg) {
                case assets: Assets =>
                  complete(OK, CacheHeader(cacheDuration), assets)

                case AssetNotFound =>
                  complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
              }

            }
        } ~
        //get all images for products
        path("product") {
          parameter('ids ?) { term =>
            import scala.util.Try
            var msg = ResourceIds(term.get.toString.split(",").map { x => Try(Integer.parseInt(x)).getOrElse(0) }.toSeq)
            onSuccess(model ? msg) {
              case assets: Assets =>
                complete(OK, CacheHeader(cacheDuration), assets)

              case AssetNotFound =>
                complete(StatusCodes.NotFound, CacheHeader(MaxAge404), "Not Found")
            }
          }
        }

    }

}

