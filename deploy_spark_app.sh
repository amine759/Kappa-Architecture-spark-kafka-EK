#!/bin/bash

# Navigate to the kappa directory
cd kappa || { echo "Failed to navigate to kappa directory."; exit 1; }

# Check if Kafka is running and create the topic if it doesn't exist
echo "Checking if Kafka is running and creating the topic if necessary..."
docker exec -it kafka bash -c "
  if ! kafka-topics --list --bootstrap-server kafka:9092 | grep -q 'main-topic'; then
    echo 'Creating Kafka topic: main-topic'
    kafka-topics --create --topic main-topic --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
  else
    echo 'Kafka topic main-topic already exists.'
  fi
"
# Step 2: Copy the assembled JAR file to the Spark master container
echo "Copying the assembled JAR file to the Spark master container..."
docker cp spark-streaming-app/target/ spark-master:/opt/bitnami/spark/work/kafka-spark-streaming
if [ $? -ne 0 ]; then
  echo "Failed to copy the JAR file to the Spark master container."
  exit 1
fi
sleep 5  # Wait for 5 seconds

# Step 3: Execute the Spark application in the Spark master container
echo "Executing the Spark application in the Spark master container..."
docker exec -it spark-master bash -c "
  cd /opt/bitnami/spark/work/
  spark-submit \
    --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
    --master spark://spark-master:7077 \
    --conf spark.driver.extraClassPath=/opt/bitnami/spark/jars/elasticsearch-spark-30_2.12-7.17.13.jar \
    --conf spark.executor.extraClassPath=/opt/bitnami/spark/jars/elasticsearch-spark-30_2.12-7.17.13.jar \
    --class sparkstreaming.SparkKafkaConsumer \
    /opt/bitnami/spark/work/kafka-spark-streaming/target/scala-2.12/spark-streaming-assembly-0.1.jar
"
if [ $? -ne 0 ]; then
  echo "Failed to execute the Spark application."
  exit 1
fi

echo "Spark application deployment completed."
