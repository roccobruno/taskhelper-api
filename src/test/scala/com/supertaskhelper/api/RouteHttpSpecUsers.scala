package com.supertaskhelper.api

import org.scalatest.{ Matchers, WordSpecLike }
import spray.testkit.ScalatestRouteTest
import concurrent.duration._
import com.supertaskhelper.router.RouteHttpService
import spray.http.StatusCodes
import com.supertaskhelper.domain._
import java.util.{Locale, Date}

import spray.httpx.SprayJsonSupport._
import com.supertaskhelper.domain.Response
import com.supertaskhelper.domain.Location
import com.supertaskhelper.domain.Task
import com.supertaskhelper.domain.Address
import spray.routing.{AuthenticationFailedRejection, MalformedQueryParamRejection, ValidationRejection}
import com.supertaskhelper.common.enums.{ACCOUNT_STATUS, ACTIVITY_TYPE, SOURCE}
import com.supertaskhelper.domain.UserRegistrationJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.UserJsonFormat._
import com.supertaskhelper.security.UserToken
import com.supertaskhelper.security.UserTokensonFormat._
import com.supertaskhelper.service.UserService

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecUsers extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService {

  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)

  val location = Location("40.1508677", "16.2848214")
  val address = Address(Option("via carlo levi API"), Option("Senise"), "Italia", Option(location),
    "85038", Option("Basilicata")
  )




  def actorRefFactory = system

  "The API Service" should {

    var userId: Option[String] = None

    "return the status message" in new RouteHttpSpecUsers {
      Get("/api/status") ~> route ~> check {
        responseAs[String] should include("\"status\": \"API-STH is running\"")
      }
    }


    val userReg = UserRegistration("test_rocco","test_rocco","test_rocco","test_rocco@msn.com",Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))

    "accept user registration " in new RouteHttpSpecUsers {
      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
     }

     Get("/api/users?id="+userId.get) ~> route ~> check {
       status should be(StatusCodes.OK)
       assert(responseAs[User].id == userId.get)
       assert(responseAs[User].email == "test_rocco@msn.com")
       assert(responseAs[User].userName == "test_rocco")
       assert(responseAs[User].accountStatus.get == ACCOUNT_STATUS.TOAPPROVE.toString)
       assert(responseAs[User].imgUrl.get == "loadphoto/USER_"+userId.get)
       assert(responseAs[User].address.get == address)
       assert(responseAs[User].address.get.address == address.address)
       assert(responseAs[User].address.get.city == address.city)
       assert(responseAs[User].address.get.postcode == address.postcode)
       assert(responseAs[User].address.get.regione == address.regione)
       assert(responseAs[User].address.get.country == address.country)
       assert(responseAs[User].address.get.location == address.location)
       assert(responseAs[User].address.get.location.get.latitude == address.location.get.latitude)
       assert(responseAs[User].address.get.location.get.longitude == address.location.get.longitude)

     }

      Get("/api/users?email=test_rocco@msn.com") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[User].id == userId.get)
        assert(responseAs[User].email == "test_rocco@msn.com")
        assert(responseAs[User].userName == "test_rocco")
        assert(responseAs[User].accountStatus.get == ACCOUNT_STATUS.TOAPPROVE.toString)
        assert(responseAs[User].address.get == address)
        assert(responseAs[User].address.get.address == address.address)
        assert(responseAs[User].address.get.city == address.city)
        assert(responseAs[User].address.get.postcode == address.postcode)
        assert(responseAs[User].address.get.regione == address.regione)
        assert(responseAs[User].address.get.country == address.country)
        assert(responseAs[User].address.get.location == address.location)
        assert(responseAs[User].address.get.location.get.latitude == address.location.get.latitude)
        assert(responseAs[User].address.get.location.get.longitude == address.location.get.longitude)

      }

      //to delete a user the token is needed
      var token = ""
      //login test
      Get("/api/login?email=test_rocco_login@msn.com&password=test_rocco") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[UserToken].userName == userReg.userName)
        assert(responseAs[UserToken].token.isEmpty == false)
        token = responseAs[UserToken].token
      }

      Delete("/api/users?id="+userId.get+"&email=test_rocco_login@msn.com&token="+token) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "should return a token when user logins in" in new RouteHttpSpecUsers {

      var token = ""
      //login test
       Get("/api/login?email=test_rocco_login@msn.com&password=test_rocco") ~> route ~> check {
         status should be(StatusCodes.OK)
         assert(responseAs[UserToken].userName == userReg.userName)
         assert(responseAs[UserToken].token.isEmpty == false)
         token = responseAs[UserToken].token
       }

      //logout test
      Get("/api/logout?email=test_rocco_login@msn.com") ~> route ~> check {
        status should be(StatusCodes.OK)
      }



    }

    /*
      - Register the user
     - check that the account is in TOAPPROVE
     - try login, it must fail
      - verifythecode
      - check that the account is in ACTIVE
      - try login it must succeed

     */
    "should register a user and than verify_code" in new  RouteHttpSpecUsers with UserService {
      val email:String = "test_rocco_reg@msn.com"
      val password:String = "test_rocco"
      val username = "test_rocco"
      val userReg = UserRegistration(username,password,"test_rocco",email,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))
      var userId: Option[String] = None
      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
      }

      Get("/api/users?email="+email) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[User].id == userId.get)
        assert(responseAs[User].email == email)
        assert(responseAs[User].accountStatus.get == ACCOUNT_STATUS.TOAPPROVE.toString)
      }

      Get("/api/login?email="+email+"&password="+password) ~> route ~> check {
        assert(rejections.size ==1)
        assert(rejections(0).isInstanceOf[AuthenticationFailedRejection])

      }

      val codeGenerated = getCodeFromEmail(email)
      Get("/api/verifycode?codeEmail="+codeGenerated.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("code verified"))
      }

      Get("/api/users?email="+email) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[User].id == userId.get)
        assert(responseAs[User].email == email)
        assert(responseAs[User].userName == username)
        assert(responseAs[User].accountStatus.get == ACCOUNT_STATUS.ACTIVE.toString)
      }

      var token = ""
      Get("/api/login?email="+email+"&password="+password) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[UserToken].userName == userReg.userName)
        assert(responseAs[UserToken].token.isEmpty == false)
        token =  responseAs[UserToken].token
      }

      //clean up db
      Delete("/api/users?id="+userId.get+"&email="+email+"&token="+token) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      removeCodeEmail(email)

      //expected error due to wrong token
      Delete("/api/users?id="+userId.get+"&email="+email+"&token=sdsdsdsdsdsdsds") ~> route ~> check {
        assert(rejections.size ==1)
        assert(rejections(0).isInstanceOf[AuthenticationFailedRejection])
      }

    }


  }
}
