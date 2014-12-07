package com.supertaskhelper.service

import com.supertaskhelper.common.domain.Country
import com.supertaskhelper.common.service.ipaddress.CountryCheckerServiceImpl
import com.typesafe.config.ConfigFactory
import org.springframework.web.client.RestTemplate

trait CountryCheckerService {
  def checkCountryForIpAddress(ip: String): Country = {
    CountryCheckerService.countryForIp(ip)
  }
}

object CountryCheckerService {

  val countryService = new CountryCheckerServiceImpl(ConfigFactory.load().getInt("api-sth.maxmind.geoip.country.userId"),
    ConfigFactory.load().getString("api-sth.maxmind.geoip.country.key"))

  countryService.setRestTemplate(new RestTemplate())

  def countryForIp(ip: String): Country = {
    countryService.checkFromIPAddress(ip)

  }

}
