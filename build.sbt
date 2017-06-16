organization := "com.unbounce.rodney"
name := "simple-traffic-monitor"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.1",
  "com.typesafe.akka" %% "akka-actor" % "2.5.1"
)

