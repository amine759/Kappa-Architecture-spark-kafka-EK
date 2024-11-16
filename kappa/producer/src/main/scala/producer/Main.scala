package producer

import akka.actor.ActorSystem
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends App {

  // Create the ActorSystem, Materializer, and ExecutionContext
  implicit val system: ActorSystem = ActorSystem("WebSocket-Kafka-Producer-System")
  implicit val materializer: Materializer = Materializer(system)
  implicit val customExecutionContext: ExecutionContext = ExecutionContext.global
  val config = Config.load()
  
  val websocketUri = config.stream
  println(s"Loaded WebSocket URI: $websocketUri type of") 

  // Create an instance of WebsocketClient with the necessary impl  icits
  val webSocketClient = new WebsocketClient()(system, materializer)
  println("Attempting to start WebSocket client...")
  println(webSocketClient.getClass)  // Check the class of webSocketClient  

  // Start the WebSocket client
  webSocketClient.startWebSocketClient(websocketUri).onComplete {
    case Success(_) => 
      println("WebSocket client started successfully.")
    case Failure(exception) => 
      println(s"Failed to start WebSocket client: ${exception.getMessage}")
  }

  // Handle system termination
  system.whenTerminated.onComplete(_ => {
    println("Shutting down system...")
    Producer.close()  // Shutdown Kafka producer
    println("Kafka producer closed.")
    system.terminate()  // Shutdown the Actor system
    println("Actor system terminated.")
  })(customExecutionContext)
}
