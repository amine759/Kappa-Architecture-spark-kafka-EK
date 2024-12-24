# Kappa-Architecture Spark-Kafka-ElasticSearch-Kibana Project

This README provides detailed instructions to set up and run the project. Follow these steps to deploy the Kappa architecture using Docker containers, Kafka, Spark, Elasticsearch, and Kibana.

## Prerequisites
Ensure you have the following installed:
- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)
- [sbt](https://www.scala-sbt.org/)

---

## Getting Started

### 1. Clone the Repository
```bash
# Clone the project repository
git clone https://github.com/amine759/Kappa-Architecture-spark-kafka-EK.git

# Navigate to the project directory
cd Kappa-Architecture-spark-kafka-EK
```

### 2. Start the Required Docker Containers
```bash
# Start the containers in detached mode
docker compose up -d
```

### 3. Verify the Containers are Running
```bash
# Check the running containers
docker ps
```

---

## Running the Producer

### 1. Navigate to the Producer Directory
```bash
cd kappa
```

### 2. Start the Producer
```bash
sbt producer/run
```

---

## Consuming Data from Kafka

### 1. Open a New Terminal and Execute the Following Command
```bash
docker compose exec kafka \
  kafka-console-consumer --bootstrap-server kafka:9092 --topic main-topic --from-beginning
```

---

## Building and Submitting the Spark Job
### 1. Deploy spark app in spark master
```bash
chmod +x deploy_spark_job.sh
./deploy_spark_app.sh
```

### 2. Alternatively do it manually if you face any issues
```bash
sbt sparkStreamingApp/clean sparkStreamingApp/assembly
```

#### 2.1: Copy the Built JAR File into the Spark Master Container
```bash
docker cp spark-streaming-app/target spark-master:/opt/bitnami/spark/work/kafka-spark-streaming
```

#### 2.2: Find the Spark Master Container Name or ID
```bash
docker ps
```

#### 2.3: Open a Bash Shell in the Spark Master Container
```bash
docker compose exec -it <container-name-or-id> bash
```

#### 2.4: Submit the Spark Job
```bash
spark-submit \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  --master spark://spark-master:7077 \
  --conf spark.driver.extraClassPath=/opt/bitnami/spark/jars/elasticsearch-spark-30_2.12-7.17.13.jar \
  --conf spark.executor.extraClassPath=/opt/bitnami/spark/jars/elasticsearch-spark-30_2.12-7.17.13.jar \
  --class sparkstreaming.SparkKafkaConsumer \
  /opt/bitnami/spark/work/kafka-spark-streaming/target/scala-2.12/spark-streaming-assembly-0.1.jar
```

---

## Verifying Data Storage in Elasticsearch

### 1. Check Data in Elasticsearch
Open a new terminal and run:
```bash
curl -X GET "localhost:9200/trades-index/_search?pretty"
```

### 2. Visualize Data in Kibana
Open your web browser and navigate to:
```
http://localhost:5601
```
You will find the `export.ndjson` import it in your kibana UI and you will Visualize Real-time updates of the transformed binance stream
---

## Troubleshooting
- Ensure all services are running by checking the container logs:
  ```bash
  docker compose logs <service-name>
  ```
- Verify that ports 9200 (Elasticsearch) and 5601 (Kibana) are not being used by other processes.

---

## Project Structure
- `docker-compose.yml`: Configuration for Docker services.
- `kappa`: Producer implementation.
- `spark-streaming-app`: Spark jobs for processing Kafka data.
---

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.

---

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Enjoy exploring the Kappa architecture!


