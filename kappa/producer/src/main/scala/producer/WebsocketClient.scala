package producer

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import java.util.concurrent.Executors

class WebsocketClient()(implicit system: ActorSystem, materializer: Materializer) {

  // Custom execution context
  val customThreadPool = Executors.newFixedThreadPool(4)
  implicit val customExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(customThreadPool)

  // Method to start the WebSocket client
  def startWebSocketClient(uri: String)(implicit system: ActorSystem, materializer: Materializer): Future[Unit] = {
    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(uri))

    // Define the source that continuously emits incoming messages to process
    val messageSource: Source[Message, _] = Source.actorRef[TextMessage.Strict](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 10,
      overflowStrategy = OverflowStrategy.fail
    )

    // Define the sink to process incoming WebSocket messages
    val messageSink: Sink[Message, _] = Sink.foreach[Message] {
      case TextMessage.Strict(text) =>
        println(s"Received message: $text")
        Producer.processWebSocketMessage(text)  // Call producer's method for handling messages
      case _ =>
        println("Received an unsupported message type")
    }

    // Create the WebSocket flow
    val ((_, closed), upgradeResponse : Future[WebSocketUpgradeResponse]) = messageSource
      .viaMat(webSocketFlow)(Keep.both)
      .toMat(messageSink)(Keep.both)
      .run()

    // Handle the WebSocketUpgradeResponse Future
    upgradeResponse.flatMap { upgrade =>
      println(s"WebSocket connection established with status: ${upgrade.response.status}")
      
      // Send subscription message after the WebSocket connection is established
      val subscribeMessage = TextMessage(
        """{
          | "method": "SUBSCRIBE",
          | "params": ["btcusdt@ticker"],
          | "id": 1
          |}""".stripMargin
      )
      
      println("Sending subscription message...")
      // Send subscription message to the WebSocket flow
      val newSource: Source[Message, _] = Source.single(subscribeMessage)
      //newSource.viaMat(webSocketFlow)(Keep.both).toMat(messageSink)(Keep.both).run()
     // WebSocket flow
      val webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = Http().webSocketClientFlow(WebSocketRequest(uri))
      Future.successful(())
    }.recover {
      case exception: Throwable =>
        println(s"Failed to upgrade WebSocket connection: ${exception.getMessage}")
    }

    // Handle the WebSocket closure
    closed.onComplete {
      case Success(_) =>
        println("WebSocket connection closed")
        system.terminate() // Terminate ActorSystem when WebSocket is closed
      case Failure(exception) =>
        println(s"WebSocket connection failed: ${exception.getMessage}")
    }(customExecutionContext)

    closed.map(_ => ())(customExecutionContext)
  }
}
