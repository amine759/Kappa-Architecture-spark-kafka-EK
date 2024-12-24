import Keys._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .aggregate(producer,sparkStreamingApp)
  .settings(
    name := "kappa",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      commonConfig
    ),
    resolvers ++= Seq(
      Resolver.mavenCentral,
      "Akka Maven Repository" at "https://repo.akka.io/releases/"
    )
  )
    
lazy val producer = (project in file("producer"))
  .settings(
    name := "websocket-producer",
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
      "com.typesafe" %% "ssl-config-core" % "0.6.1",
      munit
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    // sbt-assembly settings
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    assembly / mainClass := Some("producer.WebSocketToKafkaProducer")
  )

lazy val sparkStreamingApp = (project in file("spark-streaming-app"))
  .settings(
    name := "spark-streaming",
    scalaVersion := "2.12.18",
    version := "0.1",
    libraryDependencies ++= Seq(
      sparkCore,
      sparkSql,
      sparkSqlKafka,
      "org.elasticsearch" % "elasticsearch-spark-30_2.12" % "7.17.13"
        exclude("org.apache.spark", "spark-core_2.12")
        exclude("org.apache.spark", "spark-sql_2.12"),
      kafkaClients
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*)             => MergeStrategy.discard
      case x                                    => MergeStrategy.first
    },
    assembly / assemblyShadeRules := Seq(
      ShadeRule.rename("org.apache.kafka.**" -> "shaded.org.apache.kafka.@1").inAll
    ),
    assembly / assemblyJarName := s"${name.value}-assembly-${version.value}.jar",
    assembly / mainClass := Some("sparkstreaming.SparkKafkaConsumer")
  )
