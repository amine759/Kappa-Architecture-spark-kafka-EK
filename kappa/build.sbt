import sbt._  
import Keys._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .aggregate(producer, consumer, sparkStreamingApp)
  .settings(
    name := "kappa",
    version := "0.1.0",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      commonConfig// Common dependency across microservices
      // Add other common dependencies here if needed
    ),
    resolvers ++= Seq(
      Resolver.mavenCentral,
      "Akka Maven Repository" at "https://repo.akka.io/releases/"
    ),
  )


lazy val producer = (project in file("producer"))
  .settings(
    name := "websocket-producer",
    version := "0.1.0",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaStream,
      akkaActor,
      akkaStreamKafka,
      kafkaClients,
      logbackClassic,
      circeCore,
      circeGeneric,
      circeParser,
      slf4jApi,
      slf4jSimple,
      munit
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / mainClass := Some("producer.WebSocketToKafkaProducer") // This should point to your Main object
  )

lazy val consumer = (project in file("consumer"))
  .settings(
    name := "consumer-es",
    libraryDependencies ++= Seq(
      kafkaClients,
      logbackClassic,
      scalaCollectionCompat,
      circeCore,
      circeGeneric,
      circeParser,
      elasticsearchRestClient,
      munit
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / mainClass := Some("consumer.Consumer") // Replace with your actual main class
  )

lazy val sparkStreamingApp = (project in file("spark-streaming-app"))
  .settings(
    name := "spark-streaming-app",
    version := "0.1.0",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0",
      "org.apache.spark" %% "spark-sql" % "3.5.0",
      "org.apache.spark" %% "spark-streaming" % "3.5.0",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.0",
      logbackClassic,
      kafkaClients,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),
   // sbt-assembly settings
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    assembly / mainClass := Some("sparkstreaming.Main") // Replace with your actual Main class
  )
