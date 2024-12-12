name := "kafka-spark-streaming"
version := "0.1"
scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.0",
  // Explicit version for Elasticsearch Spark connector with exclusions
  "org.elasticsearch" % "elasticsearch-spark-30_2.12" % "7.17.13"
    exclude("org.apache.spark", "spark-core_2.12")
    exclude("org.apache.spark", "spark-sql_2.12"),
  "org.apache.kafka" % "kafka-clients" % "3.4.0"
)

// Add assembly plugin configuration for merging strategies
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _*)             => MergeStrategy.discard
  case x                                    => MergeStrategy.first
}

// Rename shaded dependencies to avoid potential conflicts
assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("org.apache.kafka.**" -> "shaded.org.apache.kafka.@1").inAll
)

// Set custom JAR name for the assembly
assembly / assemblyJarName := s"${name.value}-assembly-${version.value}.jar"
