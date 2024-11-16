package producer

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.Done
import akka.NotUsed
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import java.util.concurrent.Executors
import org.apache.kafka.clients.producer._

class WebsocketClient()(implicit system: ActorSystem, materializer: Materializer) {
  val customThreadPool = Executors.newFixedThreadPool(4)
  implicit val customExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(customThreadPool)

  def startWebSocketClient(uri: String)(implicit system: ActorSystem, materializer: Materializer): Future[Unit] = {
    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(uri))
    
    val subscribeMessage = TextMessage(
      """{
        | "method": "SUBSCRIBE",
        | "params": ["btcusdt@ticker"],
        | "id": 1
        |}""".stripMargin
    )
    
    val pingMessage = TextMessage("""{"ping": 1}""")
    val messageSink: Sink[Message, Future[Done]] = Sink.foreach[Message] {
      case message: TextMessage =>
        message.textStream.runFold("")(_ + _).map { text =>
          println(s"Received message: $text")
          Producer.processWebSocketMessage(text)
        }
      case other => 
        println(s"Received unexpected message type: $other")
    }

    // Create source of messages
    val messageSource = Source.single(subscribeMessage)
      .concat(Source.maybe[Message])

    // Run the flow with proper types
    val (upgradeResponse: Future[WebSocketUpgradeResponse], completion: Future[Done]) = 
      messageSource
        .viaMat(webSocketFlow)(Keep.right)
        .toMat(messageSink)(Keep.both)
        .run()

    // Handle connection
    upgradeResponse.onComplete {
      case Success(upgrade) =>
        println(s"WebSocket connection established with status: ${upgrade.response.status}")
      case Failure(ex) =>
        println(s"WebSocket connection failed: ${ex.getMessage}")
        system.terminate()
    }(customExecutionContext)

    // Handle completion
    completion.onComplete {
      case Success(_) =>
        println("WebSocket connection closed normally")
        system.terminate()
      case Failure(ex) =>
        println(s"WebSocket connection closed with error: ${ex.getMessage}")
        system.terminate()
    }(customExecutionContext)

    // Return a future that completes when everything is done
    completion.map(_ => ())(customExecutionContext)
  }
}
