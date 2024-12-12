package producer

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.sys.process._
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Source, Sink}
import akka.util.Timeout
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import akka.stream.Materializer
import scala.concurrent.ExecutionContext.Implicits.global

object WebSocketToKafkaProducer{

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("WebSocketToKafkaProducer")
    implicit val materializer: Materializer = Materializer(system)
    implicit val timeout: Timeout = Timeout(5.seconds)

    val config = Config.load()
    // Define the Kafka producer settings
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(config.kafkaBootstrapServers)

    // Create a Kafka sink to produce messages
    val kafkaSink = Producer.plainSink(producerSettings)

    // Define the command to execute (WebSocket stream)
    val command = Seq("websocat", "-t", config.stream)

    // Create a thread pool with 2 threads
    val executor = Executors.newFixedThreadPool(2)

    // Submit the WebSocket stream task to the thread pool
    val webSocketFuture = executor.submit(new Runnable {
      override def run(): Unit = {
        val process = command.run(ProcessLogger(
          output => {
            // For each line of WebSocket output, send it to Kafka
            println(s"Received from WebSocket: $output")
            // Use Akka Stream to process the output and send it to Kafka
            val record = new ProducerRecord[String, String]("main-topic", output)
            Source.single(record).runWith(kafkaSink)
          },
          error => println(s"Error: $error")
        ))

        // Optionally, wait for the process to finish
        process.exitValue()
      }
    })

    executor.shutdown()
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow()
      }
    } catch {
      case e: InterruptedException =>
        executor.shutdownNow()
    }

    // Ensure the actor system shuts down gracefully
    system.terminate()
  }
}
