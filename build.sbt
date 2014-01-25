name := "Api-STH"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-feature")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "twitter-repo" at "http://maven.twttr.com"

val akka = "2.2.3"
val spray = "1.2.0"

libraryDependencies ++= {
    val akkaV = "2.1.4"
    val sprayV = "1.1.0"
       Seq(
     "org.mongodb"   %% "casbah"    % "2.6.2" ,
        "com.typesafe"   %% "scalalogging-slf4j" % "1.0.1" ,
        "org.slf4j"    % "slf4j-api"    % "1.7.1" ,
        "org.slf4j"    % "log4j-over-slf4j"  % "1.7.1" ,
        "ch.qos.logback"   % "logback-classic"  % "1.0.3"  ,
        "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime" ,
        "com.rabbitmq" % "amqp-client" % "2.8.1" ,
        "com.typesafe.akka" %% "akka-actor" % akka,
        "com.typesafe.akka" %% "akka-slf4j" % akka ,
        "com.typesafe.akka" %% "akka-testkit" % akka % "test" ,
        "io.spray" % "spray-caching" % spray,
        "io.spray" % "spray-can" % spray ,
        "io.spray" % "spray-routing" % spray ,
        "io.spray" % "spray-testkit" % spray % "test",
        "io.spray" %% "spray-json" % "1.2.5",
        "org.scalatest" %% "scalatest" % "2.0" % "test",
         "org.eclipse.jetty"       %   "jetty-webapp"  % "8.1.13.v20130916"    % "container",
        "org.eclipse.jetty.orbit" %   "javax.servlet" % "3.0.0.v201112011016" % "container"  artifacts Artifact("javax.servlet", "jar", "jar"),
        "org.specs2"          %%  "specs2"        % "2.2.3" % "test"
      )
      }

scalariformSettings

seq(Revolver.settings: _*)


seq(webSettings :_*)



