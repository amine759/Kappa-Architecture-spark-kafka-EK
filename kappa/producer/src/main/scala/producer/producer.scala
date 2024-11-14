package producer

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.util.Timeout
import akka.http.scaladsl.Http
import org.apache.kafka.clients.producer._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import java.util.Properties
import org.apache.kafka.common.serialization.StringSerializer
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Producer extends App {

  // Kafka configuration
  val kafkaProps = new Properties()
  val config = Config.load() // Assuming Config is a custom class to load settings

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
    // Parse the WebSocket message as JSON
    decode[Map[String, String]](message) match {
      case Right(jsonData) =>
        val topic = jsonData.get("topic").getOrElse("default")
        val messageContent = jsonData.get("message").getOrElse("No message")
        // Send the processed message to Kafka
        sendToKafka(topic, messageContent)
      case Left(error) =>
        println(s"Failed to parse message as JSON: $error")
    }
  }

  // Start the WebSocket client
  def startWebSocketClient()(implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[Unit] = {
    // WebSocket URI
    val uri = config.stream

    // WebSocket flow: Processing incoming messages
    val flow: Flow[Message, Message, _] = Flow[Message].map {
      case TextMessage.Strict(text) =>
        // Process the WebSocket message
        processWebSocketMessage(text)
        TextMessage("Message received")
      case _ => 
        TextMessage("Unsupported message type")
    }

    // Establish WebSocket connection
    val (upgradeResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest(uri), flow)

    // Handle WebSocket connection response
    upgradeResponse.onComplete {
      case Success(upgrade) =>
        println(s"Connected to WebSocket: ${upgrade.response.status}")
      case Failure(exception) =>
        println(s"Failed to connect to WebSocket: ${exception.getMessage}")
    }
    // Handle WebSocket closure
    closed.onComplete {
      case Success(_) => 
        println("WebSocket connection closed")
      case Failure(exception) =>
        println(s"WebSocket connection failed: ${exception.getMessage}")
    }

    // Ensure that the program keeps running until the connection is closed
    closed.map(_ => ())
  
  }
  // Main entry point
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("ProducerSystem")
    implicit val materializer: Materializer = Materializer(system)
    implicit val executionContext: ExecutionContext = system.dispatcher

    // Start the WebSocket client
    startWebSocketClient()

    // Keep the application running while waiting for termination
    system.whenTerminated.onComplete(_ => {
      producer.close()
      println("Producer closed")
    })
  }
}
