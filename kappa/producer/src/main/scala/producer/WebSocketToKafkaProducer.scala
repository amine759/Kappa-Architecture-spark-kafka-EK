package producer

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Source, Sink}
import akka.stream.{Materializer, ActorMaterializer}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.{Success, Failure}
import akka.Done
import scala.concurrent.{Future, ExecutionContext, Await}
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

object WebSocketToKafkaProducer {
  def main(args: Array[String]): Unit = {
    // Create the Actor System and Materializer
    implicit val system: ActorSystem = ActorSystem("WebSocketKafkaProducer")
    implicit val materializer: Materializer = ActorMaterializer()
    val config = Config.load()

    // Kafka Producer Settings
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")
    
    val kafkaSink = Producer.plainSink(producerSettings)
    
    implicit val ec: ExecutionContext = system.dispatcher

    // Define the WebSocket command
    val command = Seq("websocat", "-t", config.stream)

    def processOutput(stdout: InputStream): Future[Done] = {
      val bufferedReader = new BufferedReader(new InputStreamReader(stdout))
      
      val source = Source.fromIterator(() => 
        Iterator.continually(bufferedReader.readLine())
          .takeWhile(_ != null)
      )

      source
        .map { line =>
          println(s"Received WebSocket line: $line")
          line
        }
        .async // Introduce an asynchronous boundary
        .runForeach { line =>
          val record = new ProducerRecord[String, String]("main-topic", line)
          Source.single(record).runWith(kafkaSink)
        }
    }

    def processError(stderr: InputStream): Unit = {
      scala.io.Source.fromInputStream(stderr).getLines().foreach(line => 
        println(s"Error: $line")
      )
    }

    // Start the WebSocket process 
    val processFuture = Future {
      val process = Process(command).run(new ProcessIO(
        _ => (), // stdin (not used)
        stdout => {
          val streamFuture = processOutput(stdout)
          Await.result(streamFuture, Duration.Inf)
        },
        processError
      ))
      process.exitValue()
    }

    // Handle process completion
    processFuture.onComplete {
      case Success(exitCode) => 
        println(s"Process exited with code: $exitCode")
        system.terminate()
      case Failure(exception) => 
        println(s"Process failed: ${exception.getMessage}")
        system.terminate()
    }

    // Wait for the actor system to terminate
    Await.result(system.whenTerminated, 1000.seconds)
  }
}
