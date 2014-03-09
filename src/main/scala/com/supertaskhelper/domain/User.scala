package com.supertaskhelper.domain

import spray.json._
import java.util.{ Date, Locale }
import java.text.SimpleDateFormat
import com.supertaskhelper.domain.UserRegistration
import com.supertaskhelper.domain.User
import com.supertaskhelper.common.enums.SOURCE
import com.supertaskhelper.domain.search.Searchable
import spray.json._
import DefaultJsonProtocol._

/**
 * Created with IntelliJ IDEA.
 * User: r.bruno@london.net-a-porter.com
 * Date: 15/02/2014
 * Time: 20:40
 * To change this template use File | Settings | File Templates.
 */
/*
{
    "_id": {
        "$oid": "52778867e4b001e9a2386025"
    },
    "_class": "com.supertaskhelper.common.domain.User",
    "onlineSkills": [],
    "offlineSkills": [],
    "meansOfTransportations": [
        {
            "_id": {
                "$oid": "525baecde4b01e75da3a4260"
            },
            "title_en": "Bike",
            "title_it": "Moto",
            "order": 2,
            "description": "bike",
            "type": "MT"
        }
    ],
    "daysAvailable": [
        "Saturday"
    ],
    "timesAvailable": [
        "Evening"
    ],
    "username": "Matteo",
    "password": "WEqfC6dwpkazhYmN1xvkp1IQEiAPRaQeqmIzHDy+9dk=$Bzd9ZqquyZe6hZt5BEH1tTqJFMNYH9sOOeC8wBD0UAM=",
    "firstName": "Matteo",
    "lastName": "Rava",
    "email": "matteorava100@gmail.com",
    "address": {
        "address": "Via Santa Giulia, Torino, TO, Italia 18 ",
        "city": "Torino",
        "country": "United_Kingdom",
        "location": {
            "longitude": "7.697274900000025",
            "latitude": "45.070592"
        },
        "postcode": "10124",
        "regione": ""
    },
    "rating": 0,
    "accountStatus": "ACTIVE",
    "bio": " Ma ora arriva finalmente una buona notizia da un report compilato dalla Netherlands Environment Assessment Agency e dallo European Commission’s Joint Research Centre: per la prima volta, qualcosa si muove e ci si può timidamente rallegrare.\r\n\r\nCRESCITA TIMIDA - La notizia è che le emissioni crescono sempre, ma ben più lentamente rispetto agli anni precedenti e che il ritmo di crescita medio del 2012 è circa la metà di quello registrato nel precedente decennio. Il tasso di crescita delle emissioni di CO2 per quest’anno è stato dell’1,4%, nonostante il tasso di crescita globale dell’economia sia stato del 3,5 per cento. Si è verificato dunque una sorta di allontanamento dalla crescita dell’economia, dopo anni che i due rispettivi trend marciavano allineati, dimostrando che la concentrazione di anidride carbonica è indubbiamente imputabile all’attività umana.\r\n\r\nBUONI E CATTIVI - Tra le tante informazioni che fornisce il report c’è anche quella riguardante le nazioni più buone e quelle più cattive: tre sono le aree geografiche che rimangono responsabili di più delle metà delle emissioni totali. Tra queste figura la Cina, seguita a ruota dagli Stati Uniti e dall’Unione Europea. Le emissioni della prima hanno avuto un aumento del 3 per cento, che di per sé non è una bella notizia, ma che paragonato all’aumento del 10% rilevato nelle misurazioni del decennio passato fa quasi cantar vittoria.",
    "fbBudget": false,
    "twitterBudget": false,
    "linkedInBudget": false,
    "securityDocVerified": false,
    "emailVerified": false,
    "idDocVerified": false,
    "webcamVerified": false,
    "mobileVerified": false,
    "securityDocSubmitted": false,
    "idDocSubmitted": false,
    "profileImage": false,
    "mobile": "3496545545",
    "createdDate": {
        "$date": "2013-12-17T00:21:42.579Z"
    },
    "skillsMap": {
        "online_private_lesson": true,
        "online_private_lesson_ph": "10",
        "online_teaching_langs": true,
        "online_teaching_langs_ph": "10",
        "online_buying": true,
        "online_buying_ph": "8",
        "online_it_help": true,
        "online_it_help_ph": "8",
        "online_office_help": true,
        "online_office_help_ph": "8",
        "online_other": false,
        "online_creativ_artistic": false,
        "offline_shopping": true,
        "offline_shopping_ph": "8",
        "offline_delivery": true,
        "offline_delivery_ph": "8",
        "offline_cleaning": true,
        "offline_cleaning_ph": "8",
        "offline_babysitting": true,
        "offline_babysitting_ph": "8",
        "offline_dogsitting": false,
        "offline_hanydman": false,
        "offline_movin_packing": false,
        "offline_other": false
    },
    "withAccount": false,
    "STH": true,
    "okForSMS": false,
    "okForEmailsAboutComments": true,
    "okForEmailsAboutBids": true,
    "sourcesOfInfo": [
        "Email_Marketing"
    ],
    "averageRating": 0,
    "numOfFeedbacks": 0
}
"fbBudget": false,
    "twitterBudget": false,
    "linkedInBudget": false,
    "securityDocVerified": false,
    "emailVerified": false,
    "idDocVerified": false,
    "webcamVerified": false,
 */
case class User(userName: String, isSTH: Boolean, email: String, password: String, id: String, imgUrl: Option[String],
  distance: Option[String], address: Option[Address], bio: Option[String], fbBudget: Option[Boolean], twitterBudget: Option[Boolean],
  linkedInBudget: Option[Boolean], securityDocVerified: Option[Boolean], emailVerified: Option[Boolean], idDocVerified: Option[Boolean],
  webcamVerified: Option[Boolean])
    extends Searchable
object UserJsonFormat extends DefaultJsonProtocol {
  implicit val locationFormat = jsonFormat2(Location)
  implicit val addressFormat = jsonFormat6(Address)
  implicit val userFormat = jsonFormat16(User)
}

case class UserRegistration(userName: String, password: String, confirmPassword: String, email: String,
    language: Option[Locale], source: Option[String]) {

  require(!userName.isEmpty, "username must not be empty")
  require(!email.isEmpty, "email must not be empty")
  require(!password.isEmpty, "password must not be empty")
  require(!confirmPassword.isEmpty, "confirmPassword must not be empty")
  require(password == confirmPassword, "password and confirmpassword must be equals")
  require(source.isEmpty || SOURCE.valueOf(source.get) != null, "source value not recognized")

}

object UserRegistrationJsonFormat extends DefaultJsonProtocol {
  implicit object LocaleFormat extends RootJsonFormat[Locale] {
    def write(c: Locale) = {

      JsString(c.toString)
    }

    def read(value: JsValue) = value match {
      case JsString(value) => {

        value match {
          case "it" => Locale.ITALIAN
          case "en" => Locale.UK
          case _ => Locale.ITALIAN
        }

      }

      case _ => deserializationError("Locale expected")
    }
  }
  implicit val userRegistrationFormat = jsonFormat6(UserRegistration)
}