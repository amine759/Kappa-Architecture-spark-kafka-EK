import sbt._

object Dependencies {
  // Testing
  val munit = "org.scalameta" %% "munit" % "0.7.29" % Test

  // Kafka Dependencies
  val kafkaClients = "org.apache.kafka" % "kafka-clients" % "3.5.2"

  // Logging
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.10"

  // Scala Compatibility
  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0"

  // Circe for JSON
  val circeCore = "io.circe" %% "circe-core" % "0.14.3"
  val circeGeneric = "io.circe" %% "circe-generic" % "0.14.3"
  val circeParser = "io.circe" %% "circe-parser" % "0.14.3"

  // Elasticsearch
  val elasticsearchRestClient = "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.0"

  // Configuration
  val commonConfig = "com.typesafe" % "config" % "1.4.1"

  // Akka HTTP for WebSocket
  val AkkaVersion = "2.7.0"
  val AkkaHttpVersion = "10.5.0"
  // Akka HTTP for WebSocket
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion 
  // Akka Streams for handling streaming data
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % AkkaVersion
   // Akka HTTP for WebSocket
}
