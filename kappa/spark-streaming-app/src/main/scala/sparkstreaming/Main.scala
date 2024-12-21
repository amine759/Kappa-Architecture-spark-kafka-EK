package sparkstreaming

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.{StreamingQuery, OutputMode, Trigger}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.OutputMode

object SparkKafkaConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("BinanceTradeConsumer")
      .master("local[*]")
      .config("spark.es.nodes", "elasticsearch") // Elasticsearch host
      .config("spark.es.port", "9200") // Elasticsearch port
      .config("spark.es.index.auto.create", "true") // Automatically create index  
      .config("spark.sql.caseSensitive", true) // .config("spark.es.nodes.wan.only", "true")  Enable WAN mode (optional)
      .config("spark.sql.shuffle.partitions", "200")
      .config("spark.dynamicAllocation.enabled", "true")
      .config("spark.es.nodes.wan.only", "true")
      .config("spark.es.mapping.date.rich", "false")
      .config("spark.es.index.read.missing.as.empty", "true")
      .config("es.index.auto.create", "true")
      .config("es.nodes", "elasticsearch")
      .config("es.port", "9200")
      .getOrCreate()

    import spark.implicits._

    // Define the Kafka source
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:29092")
      .option("subscribe", "main-topic")
      .option("startingOffsets", "earliest")
      .load()

    // Define the schema for the JSON data
    val jsonSchema = StructType(Seq(
      StructField("e", StringType, nullable = true),
      StructField("E", LongType, nullable = true),
      StructField("s", StringType, nullable = true),
      StructField("t", LongType, nullable = true),
      StructField("p", StringType, nullable = true),
      StructField("q", StringType, nullable = true),
      StructField("T", LongType, nullable = true),
      StructField("m", BooleanType, nullable = true),
      StructField("M", BooleanType, nullable = true)
    ))

    // Convert the Kafka value (JSON string) to a DataFrame
    val jsonDF = kafkaDF.selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value"), jsonSchema).as("data"))
      .select("data.*")

    // Renaming columns
    val renamedDF = jsonDF
      .withColumnRenamed("e", "event_type")
      .withColumnRenamed("E", "event_timestamp")
      .withColumnRenamed("s", "symbol")
      .withColumnRenamed("t", "trade_timestamp")
      .withColumnRenamed("p", "price")
      .withColumnRenamed("q", "quantity")
      .withColumnRenamed("T", "trade_time")
      .withColumnRenamed("m", "market_status")
      .withColumnRenamed("M", "market_condition")

    // Casting and converting columns
    val castedDF = renamedDF
      .withColumn("price", $"price".cast("double"))
      .withColumn("quantity", $"quantity".cast("double"))
      .withColumn("event_timestamp", $"event_timestamp".cast("long"))
      .withColumn("trade_timestamp", $"trade_timestamp".cast("long"))
      .withColumn("trade_time", from_unixtime($"trade_time" / 1000).cast("timestamp")) // Convert to DateType
      .withColumn("trade_time", date_format($"trade_time", "yyyy-MM-dd HH:mm:ss").cast("string")) // Convert timestamp to formatted date

    // Add Trade Direction (Buy/Sell)
    val enrichedDF = castedDF
      .withColumn("trade_direction", when(col("market_status") === true, "Sell").otherwise("Buy"))

    // Add Trade Value (price * quantity)
    val valueDF = enrichedDF
      .withColumn("trade_value", col("price") * col("quantity"))

    // Detect Anomalies Based on a Threshold (e.g., price > 100,000)
    val withAnomalies = valueDF
      .withColumn("is_anomaly", when(col("price") > 100000, true).otherwise(false))

   
    // Write aggregated data to Elasticsearch
    val query: StreamingQuery =  withAnomalies
      .writeStream
      .outputMode("append")  // Append mode works with aggregation
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.write
          .format("org.elasticsearch.spark.sql")
          .option("es.resource", "trades-index")
          .option("es.nodes", "elasticsearch")
          .option("es.port", "9200")
          .mode("append")
          .option("es.id", "trade_timestamp") // Optional: set a unique document ID
          .save()
      }
      .option("checkpointLocation", "/opt/spark/checkpoint/aggregated-trades")  // Specify checkpoint location
      .start()

    // Await termination
    query.awaitTermination()
  }
}
