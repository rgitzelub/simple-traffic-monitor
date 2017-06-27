organization := "com.unbounce.rodney"
name := "simple-traffic-monitor"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
//  "joda-time" % "joda-time" % "2.9.8",

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.typesafe.akka" %% "akka-http" % "10.0.8",

  "com.typesafe.akka" %% "akka-actor" % "2.4.19",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.19"
)

