package producer

import akka.actor.ActorSystem
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("WebSocket-Kafka-Producer-System")
  implicit val materializer: Materializer = Materializer(system)
  implicit val customExecutionContext: ExecutionContext = ExecutionContext.global
  val config = Config.load()

  val websocketUri = config.stream
  println(s"Loaded WebSocket URI: $websocketUri type of ${websocketUri.getClass()}")
  
  val webSocketClient = new WebsocketClient()
  println("Attempting to start WebSocket client...")

  webSocketClient.startWebSocketClient(websocketUri).onComplete {
    case Success(_) => 
    println("WebSocket client started successfully.")
    case Failure(exception) => 
    println(s"Failed to start WebSocket client: ${exception.getMessage}")
  }
  system.whenTerminated.onComplete(_ => {
    println("Shutting down system...")
    Producer.close()  // Shutdown Kafka producer
    println("Kafka producer closed.")
    system.terminate()  // Shutdown the Actor system
    println("Actor system terminated.")
  })(customExecutionContext)

}

