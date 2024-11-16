package producer

import org.apache.kafka.clients.producer._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import java.util.Properties
import org.apache.kafka.common.serialization.StringSerializer
import scala.util.{Failure, Success}
import io.circe._
import io.circe.parser._

case class WebSocketMessage(
  e: String,
  E: Long,
  s: String,
  t: Long,
  p: String,
  q: String,
  T: Long,
  m: Boolean,
  M: Boolean
)

object Producer {

  val kafkaProps = new Properties()
  val config = Config.load() // assuming Config is a custom class to load settings

  kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers)
  kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

  // Kafka producer instance
  val producer = new KafkaProducer[String, String](kafkaProps)
  
  // Method to send messages to Kafka
  def sendToKafka(topic: String, message: String): Unit = {
    val record = new ProducerRecord[String, String](topic, null, message)
    producer.send(record, (metadata, exception) => {
      if (exception != null) {
        println(s"Error sending message to Kafka: ${exception.getMessage}")
      } else {
        println(s"Message sent to Kafka topic ${metadata.topic()} at offset ${metadata.offset()}")
      }
    })
  }
  // Method to process WebSocket messages
  def processWebSocketMessage(message: String): Unit = {
    decode[WebSocketMessage](message) match {
      case Right(jsonData) =>
        println(s"Parsed message: $jsonData")
        val topic = jsonData.s
        val messageContent = s"Price: ${jsonData.p}, Quantity: ${jsonData.q}"
        sendToKafka(topic, messageContent)
      case Left(error) =>
        println(s"Failed to parse message as JSON: $error")
    }
  }
  def close(): Unit = {
      producer.close()
      println("Kafka producer closed")
    }

}


