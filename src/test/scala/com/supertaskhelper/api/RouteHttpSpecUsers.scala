package com.supertaskhelper.api

import java.util.{Date, Locale}

import com.supertaskhelper.common.enums.{ACCOUNT_STATUS, SOURCE, TASK_TYPE}
import com.supertaskhelper.domain.AccountJsonFormat._
import com.supertaskhelper.domain.FeedbacksJsonFormat._
import com.supertaskhelper.domain.ResponseJsonFormat._
import com.supertaskhelper.domain.TaskCategoriesJsonFormat._
import com.supertaskhelper.domain.UserJsonFormat._
import com.supertaskhelper.domain.UserRegistrationJsonFormat._
import com.supertaskhelper.domain.{Address, Location, Response, _}
import com.supertaskhelper.router.RouteHttpService
import com.supertaskhelper.security.UserToken
import com.supertaskhelper.security.UserTokensonFormat._
import com.supertaskhelper.service.UserService
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.AuthenticationFailedRejection
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 16/03/2014
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class RouteHttpSpecUsers extends WordSpecLike with ScalatestRouteTest with Matchers with RouteHttpService  {

  implicit val routeTestTimeout = RouteTestTimeout(15 seconds)

  val location = Location("40.1508677", "16.2848214")
  val address = Address(Option("via carlo levi API"), Option("Senise"), "Italia", Option(location),
    Option("85038"), Option("Basilicata")
  )




  def actorRefFactory = system

  "The API Service" should {

    var userId: Option[String] = None

    "return the status message" in new RouteHttpSpecUsers {
      Get("/api/status") ~> route ~> check {
        responseAs[String] should include("\"status\": \"API-STH is running\"")
      }
    }


    val userEmail: String = "test_rocco@msn.com"
    val userReg = UserRegistration("test_rocco","test_lastname","test_rocco","test_rocco",userEmail,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))

    "accept user registration " in new RouteHttpSpecUsers {
      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
      }

      val acc = Account(userId.get,userEmail,Some("099887756"),true,true,true,"130780",None,false,Some("paypal@email.com"))

      Post("/api/users/account",acc) ~> route ~> check {
        status should be(StatusCodes.OK)
      }

      Get("/api/users/account/"+userId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Account].userId == userId.get)
        assert(responseAs[Account].email == userEmail)
        assert(responseAs[Account].mobile.getOrElse("0") == "099887756")
        assert(responseAs[Account].sms == true)
        assert(responseAs[Account].okForEmailsAboutBids == true)
        assert(responseAs[Account].okForEmailsAboutComments == true)
        assert(responseAs[Account].hasFacebookConnection == false)
        assert(responseAs[Account].paypalEmail == Some("paypal@email.com"))
      }

      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
     }

     Get("/api/users?id="+userId.get) ~> route ~> check {
       status should be(StatusCodes.OK)
       assert(responseAs[User].id == userId.get)
       assert(responseAs[User].email == userEmail)
       assert(responseAs[User].userName == "test_rocco")
       assert(responseAs[User].lastName == "test_lastname")
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
        assert(responseAs[User].email == userEmail)
        assert(responseAs[User].userName == "test_rocco")
          assert(responseAs[User].lastName == "test_lastname")
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

      val userReg2 = UserRegistration("test_rocco","test_lastname","test_rocco","test_rocco",userEmail,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),None)

      Post("/api/users",userReg2) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[Response].message.contains("Resource Added"))
        userId = Option(responseAs[Response].id)
      }

      Get("/api/users?id="+userId.get) ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[User].id == userId.get)
        assert(responseAs[User].email == userEmail)
        assert(responseAs[User].userName == "test_rocco")
        assert(responseAs[User].accountStatus.get == ACCOUNT_STATUS.TOAPPROVE.toString)
        assert(responseAs[User].imgUrl.get == "loadphoto/USER_"+userId.get)
        assert(!responseAs[User].address.isDefined)


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


    // Based on a user alsredy existing in the DB with 1 feeback a 1 offline skill and 2 online ones
    "should return a list of feebaks and skills" in new RouteHttpSpecUsers {

      var token = ""
      //login test
      Get("/api/users/feedbacks?userId=5383735e03641e76a78bded1") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(!responseAs[Feedbacks].feedbacks.isEmpty)
        assert(responseAs[Feedbacks].feedbacks(0).rating ==4)
        assert(responseAs[Feedbacks].feedbacks(0).taskId == "53335517e4b0e724b3b614c3")
        assert(responseAs[Feedbacks].feedbacks(0).description == "veloce e affiabile. grosso aiuto")
        assert(responseAs[Feedbacks].feedbacks(0).userId == "526d47a2e4b065aeb452d43f")
        assert(responseAs[Feedbacks].feedbacks(0).createdDate.before(new Date()))

      }

      //logout test
      Get("/api/users/skills?userId=5383735e03641e76a78bded1") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(!responseAs[TaskCategories].categories.isEmpty)
        var countOnlines = 0
        var countOfflines = 0

        for ( x <- responseAs[TaskCategories].categories){
          if(x.categoryType.get == TASK_TYPE.OFFLINE.toString) {
            countOfflines = countOfflines + 1
            assert(x.id == "52515bb0e4b094388a43ca39")
            assert(x.title_it.get == "tuttofare")
            assert(x.title_en.get == "handyman")
            assert(x.tariff.get == "10")
          }
          else
            countOnlines = countOnlines + 1
        }
        assert(countOfflines == 1)
        assert(countOnlines == 3)


      }



    }


    "should return an error due to email already used" in new  RouteHttpSpecUsers with UserService {
      val email:String = "test_rocco_login@msn.com"
      val password:String = "test_rocco"
      val username = "test_rocco"
      val userReg = UserRegistration(username,"test_lastname",password,"test_rocco",email,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))
      var userId: Option[String] = None
      Post("/api/users",userReg) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
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
      val userReg = UserRegistration(username,"test_lastname",password,"test_rocco",email,Option(Locale.ITALIAN),Option(SOURCE.MOBILE_ANDROID.toString),Option(address))
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
