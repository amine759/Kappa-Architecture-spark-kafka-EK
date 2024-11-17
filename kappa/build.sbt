import sbt._  
import Keys._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .aggregate(producer)
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
    name := "kappa",
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
      akkaSpra,
      munit
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / mainClass := Some("producer.WebSocketToKafkaProducer") // This should point to your Main object
  )

