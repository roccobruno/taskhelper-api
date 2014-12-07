package com.supertaskhelper.service

import com.supertaskhelper.common.domain.Country
import com.supertaskhelper.common.service.ipaddress.CountryCheckerServiceImpl
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate

trait CountryCheckerService {
  val logger = LoggerFactory.getLogger(classOf[CountryCheckerService])

  def checkCountryForIpAddress(ip: Option[String]): Option[Country] = {
    if (ip.isDefined) {
      var res: Option[Country] = None
      try {
        res = Some(CountryCheckerService.countryForIp(ip.get))
      } catch {
        case e: Throwable => {
          logger.error(s"Error checking for country for given ipaddress ${ip}")
          logger.error(s"Error ${e}")
          res
        }
      }

      res
    } else
      None
  }
}

object CountryCheckerService {

  val countryService = new CountryCheckerServiceImpl(ConfigFactory.load().getInt("api-sth.maxmind.geoip.country.userid"),
    ConfigFactory.load().getString("api-sth.maxmind.geoip.country.key"))

  countryService.setRestTemplate(new RestTemplate())

  def countryForIp(ip: String): Country = {

    countryService.checkFromIPAddress(ip)

  }

}
