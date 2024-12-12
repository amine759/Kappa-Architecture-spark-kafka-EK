from pyspark.sql import SparkSession

# Initialize Spark session
spark = SparkSession.builder.appName("Kafka Batch Processing").config('spark.jars.packages',"org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0").getOrCreate()

# Kafka configuration
kafka_broker = "172.18.0.5:9092"  # Correct Kafka broker IP address
kafka_topic = "main-topic"

# Read data from Kafka as a batch DataFrame
df = spark.read \
    .format("kafka") \
    .option("kafka.bootstrap.servers", kafka_broker) \
    .option("subscribe", kafka_topic) \
    .option('startingOffsets', 'earliest') \
    .load()

# Convert the Kafka data to a string (optional, depending on your data)
df = df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
print("piiiiiiiiiiiiiiiiiiiiiiiste")
# Show the first few records
df.show()
