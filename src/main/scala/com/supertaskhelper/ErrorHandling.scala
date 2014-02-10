//package com.supertaskhelper
//
//import spray.httpx.SprayJsonSupport
//import akka.event.LoggingAdapter
//import spray.routing.{MethodRejection, MissingQueryParamRejection, RejectionHandler, ExceptionHandler}
//import com.paypal.core.rest.HttpMethod
//import spray.http.HttpMethod
//
///**
// * Created with IntelliJ IDEA.
// * User: r.bruno@london.net-a-porter.com
// * Date: 08/02/2014
// * Time: 18:28
// * To change this template use File | Settings | File Templates.
// */
//trait ErrorHandling extends SprayJsonSupport {
//
//  /**
//   * Mix in with ActorLogging to get an implementation for this
//   */
//  def log: LoggingAdapter
//
//  def yetiExceptionHandler =
//    ExceptionHandler {
//      case e =>
//        log.error(e, "Unable to complete request")
//        complete(InternalServerError, "Unable to complete your request")
//    }
//
//  def yetiRejectionHandlerSorted = RejectionHandler {
//
//    case MissingQueryParamRejection(name) :: _ => complete(BadRequest, s"$name query param is required")
//    case MethodRejection(HttpMethod(name, _, _, _)) :: _ => complete(BadRequest, s"Method not supported. Did you mean $name")
//    case MissingHeaderRejection(name) :: _ => complete(BadRequest, s"$name header is required")
//    case Nil => complete(NotFound, "The resource could not be found")
//    case x :: _ => {
//      log.error(s"Uncaught Rejection: ${x.toString}")
//      complete(InternalServerError, "unhandled error")
//    }
//  }
//
//  def complete(status: StatusCode, description: String): Route = ctx => {
//    ctx.complete(ErrorDto(description))
//  }
//
//  /**
//   * When we reject a request for multiple reasons, use a lower number to prioritise Rejections, so they will be shown
//   * to the user over other rejections
//   */
//  val rejectionOrder: Rejection => Int = {
//    case MethodRejection(_) => 2
//    case _ => 1
//  }
//
//  def yetiRejectionHandler = RejectionHandler {
//    case rejections => yetiRejectionHandlerSorted(rejections.sortBy(rejectionOrder))
//  }
//}