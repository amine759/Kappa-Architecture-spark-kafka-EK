from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json, expr
from pyspark.sql.types import StructType, StructField, StringType, BooleanType, LongType, DoubleType

# Initialize SparkSession
spark = SparkSession.builder \
    .appName("Kafka Spark Transformation") \
    .getOrCreate()

# Define schema for the JSON message (Excluding trade_id 't')
json_schema = StructType([
    StructField("s", StringType()),  # Symbol
    StructField("p", DoubleType()),  # Price
    StructField("q", DoubleType()),  # Quantity
    StructField("T", LongType()),  # Trade time in milliseconds
    StructField("m", BooleanType())  # Is market maker
])

# Create the streaming DataFrame with all transformations in one chain
df_transformed = spark.readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "172.18.0.5:9092") \
    .option("subscribe", "main-topic") \
    .load() \
    .selectExpr("CAST(value AS STRING) as json_string") \
    .withColumn("parsed", from_json(col("json_string"), json_schema)) \
    .select(
        col("parsed.s").alias("symbol"),  # Symbol
        col("parsed.p").alias("price"),  # Price
        col("parsed.q").alias("quantity"),  # Quantity
        expr("CAST(parsed.T / 1000 AS TIMESTAMP) as trade_timestamp"),  # Convert 'T' to timestamp
        col("parsed.m").alias("is_market_maker")  # Is market maker
    )

# Debug: Print schema to verify structure
df_transformed.printSchema()

# Write transformed data to the console for verification
query = df_transformed.writeStream \
    .outputMode("append") \
    .format("console") \
    .start()

try:
    query.awaitTermination()
except KeyboardInterrupt:
    print("Streaming interrupted by user.")
finally:
    query.stop()
    spark.stop()
