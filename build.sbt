name := "spotify-auth-adapter"

version := "0.1.0"

scalaVersion := "2.12.12"

val sVersion = "2.12.12"
val akkaVersion = "2.6.17"
val jsonVersion = "3.6.9"
val xtractVersion = "2.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.json4s" %% "json4s-native" % jsonVersion,
  "org.json4s" %% "json4s-jackson" % jsonVersion,
  "com.lucidchart" %% "xtract" % xtractVersion,
  "com.lucidchart" %% "xtract-testing" % xtractVersion % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "com.github.scredis" %% "scredis" % "2.3.4",
  "kz.mounty"         %% "mounty-domain"    % "0.1.1-SNAPSHOT",
  "org.scalaj"        %% "scalaj-http"      % "2.4.2",
  "ch.megard" %% "akka-http-cors" % "1.1.2"
)
