package sparkstreaming

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger

object Main {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder
      .appName("SparkStreamingApp")
      .master("spark://spark-master:7077") // Use " in cluster mode
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // Kafka Stream Configuration
    val kafkaStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "broker:9092") // Replace with your broker address
      .option("subscribe", "main-topic") // Replace with your Kafka topic
      .option("startingOffsets", "earliest") // Starting position in Kafka topic
      .load()

    // Deserialize messages
    import spark.implicits._
    val messages = kafkaStream.selectExpr("CAST(value AS STRING)").as[String]

    // Print the incoming messages to the console
    val query = messages.writeStream
      .outputMode("append") // Can be "append", "update", or "complete"
      .format("console")    // Output to the console for debugging
      .trigger(Trigger.ProcessingTime("10 seconds")) // Trigger interval
      .start()

    println("Spark Streaming application has started!")

    // Await termination
    query.awaitTermination()
  }
}
