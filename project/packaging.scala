import sbt._
import com.typesafe.sbt.packager.Keys._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._
import scala.util.Properties

object packaging {

  val settings: Seq[Setting[_]] = packagerSettings ++ deploymentSettings ++ Seq(
    name in Rpm := name.value.toLowerCase.replace(' ', '-'),
    version in Rpm := Properties.envOrElse("BUILD_NUMBER", "0"),
    packageSummary in Linux := description.value,
    rpmGroup := Some("Scala apps"),
    rpmRelease := "1",
    rpmVendor := "Net-A-Porter",
    rpmLicense := Some("Copyright (c) Net-A-Porter"),
    linuxPackageMappings <+= (target, name) map { (bd, n) =>
      val pkgName = n.toLowerCase.replace(' ', '-')
      (packageMapping((bd / s"scala-2.10/${pkgName}_2.10-1.war") -> s"/opt/app/$pkgName/deploy/tomcat/$pkgName.war")
        withUser "root" withGroup "root" withPerms "0755")
    }
  )
}