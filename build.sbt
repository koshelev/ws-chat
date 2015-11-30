name := "ws-chat"

version := "1.0"

val commonSettings = Seq(
  scalaVersion := "2.11.7",
  scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.7", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint", "-language:higherKinds")
)

lazy val akkaVersion = "2.4.0"

lazy val akkaHttpVersion = "2.0-M1"

lazy val gatlingVersion = "2.1.7"

lazy val Akka = Seq(
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaHttpVersion,
  "com.typesafe.akka" % "akka-http-experimental_2.11" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

lazy val GatlingLib = Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test",
  "io.gatling"            % "gatling-test-framework"    % gatlingVersion % "test"
)

lazy val Netty = "io.netty" % "netty-all" % "4.0.33.Final"

lazy val nettyChat = project.in(file("netty-chat"))
  .settings(commonSettings: _*)
  .settings(resolvers += "Confluent" at "http://packages.confluent.io/maven")
  .settings(libraryDependencies ++= Seq(Netty, "ch.qos.logback" % "logback-classic" % "1.1.3") )
  .enablePlugins(JavaAppPackaging, UniversalPlugin)

lazy val akkaChat = project.in(file("akka-chat"))
  .settings(commonSettings: _*)
  .settings(Revolver.settings)
  .settings(libraryDependencies ++= Akka)
  .enablePlugins(JavaAppPackaging, UniversalPlugin)

lazy val loadTest = project.in(file("load-test"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= GatlingLib )
  .enablePlugins(GatlingPlugin)

lazy val root = project.in(file(".")).aggregate(nettyChat, akkaChat)