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
    resolvers += "Maven Central" at "https://repo1.maven.org/maven2/"
  )


lazy val producer = (project in file("producer"))
  .settings(
    name := "producer-service",
    libraryDependencies ++= Seq(
      kafkaClients,
      logbackClassic,
      scalaCollectionCompat,
      circeCore,
      circeGeneric,
      circeParser,
      munit,
      akkaHttp,    // Added Akka HTTP dependency for WebSocket communication
      akkaStream    // Added Akka Streams dependency for streaming data handling
      // Add producer-specific dependencies here
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    assembly / mainClass := Some("producer.Producer") // Replace with your actual main class
  )
