package consumer

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.{Collections, Properties}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import org.apache.kafka.common.errors.WakeupException

object Consumer extends App {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // Load configuration
  val config = Config.load()

  // Initialize Kafka consumer properties
  val kafkaProps = new Properties()
  kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers)
  kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, config.kafkaGroupId)
  kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") // Start reading at the earliest offset

  // Create Kafka consumer
  val consumer = new KafkaConsumer[String, String](kafkaProps)
  consumer.subscribe(Collections.singletonList(config.kafkaTopic))
  logger.info(s"Subscribed to Kafka topic: ${config.kafkaTopic}")

  // Initialize Elasticsearch client
  val elasticsearchClient: RestHighLevelClient = ElasticsearchClientProvider.createClient(config)

  // Graceful shutdown handling
  sys.addShutdownHook {
    logger.info("Shutting down consumer and Elasticsearch client...")
    Try(consumer.wakeup()) match {
      case Success(_) => logger.info("Consumer wakeup signal sent.")
      case Failure(exception) => logger.error("Error sending wakeup signal to consumer.", exception)
    }
    Try(consumer.close()) match {
      case Success(_) => logger.info("Kafka consumer closed.")
      case Failure(exception) => logger.error("Error closing Kafka consumer.", exception)
    }
    Try(elasticsearchClient.close()) match {
      case Success(_) => logger.info("Elasticsearch client closed.")
      case Failure(exception) => logger.error("Error closing Elasticsearch client.", exception)
    }
    logger.info("Shutdown complete.")
  }

  // Poll and process messages
  try {
    logger.info("Starting message consumption...")
    while (true) {
      val records = consumer.poll(Duration.ofMillis(1000))
      if (!records.isEmpty) {
        logger.info(s"Polled ${records.count()} records from Kafka.")
      }
      records.asScala.foreach { record =>
        val message = record.value()
        val offset = record.offset()
        logger.debug(s"Consumed message at offset $offset: $message")

        // Create an index request for Elasticsearch
        val indexRequest = new IndexRequest(config.elasticsearchIndex)
          .id(offset.toString) // Optionally specify an ID
          .source(Map("message" -> message).asJava) // Specify the document to index

        // Index the document with error handling
        Try(elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT)) match {
          case Success(response) =>
            logger.info(s"Indexed document with ID: ${response.getId}")
          case Failure(exception) =>
            logger.error(s"Failed to index document at offset $offset: ${exception.getMessage}", exception)
        }
      }
    }
  } catch {
    case e: WakeupException =>
      logger.info("Received shutdown signal (WakeupException).")
    case e: Exception =>
      logger.error(s"Unexpected error in consumer: ${e.getMessage}", e)
  } finally {
    Try(consumer.close()) match {
      case Success(_) => logger.info("Kafka consumer closed in finally block.")
      case Failure(exception) => logger.error("Error closing Kafka consumer in finally block.", exception)
    }
    Try(elasticsearchClient.close()) match {
      case Success(_) => logger.info("Elasticsearch client closed in finally block.")
      case Failure(exception) => logger.error("Error closing Elasticsearch client in finally block.", exception)
    }
  }
}
