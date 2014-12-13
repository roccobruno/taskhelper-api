package com.supertaskhelper.service

import com.supertaskhelper.common.service.ipaddress.CountryCheckerServiceImpl
import com.typesafe.config.ConfigFactory
import org.springframework.web.client.RestTemplate

trait CountryCheckerService extends Service {

  def checkCountryForIpAddress(ip: Option[String]): Option[String] = {
    if (ip.isDefined) {
      var res: Option[String] = None
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

  def countryForIp(ip: String): String = {

    countryService.getCountryFromIPAddress(ip).getCode

  }

}
