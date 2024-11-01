import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.Materializer
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

// Define the case class to match the JSON structure
case class TradeData(e: String, E: Long, s: String, t: Long, p: String, q: String, T: Long, m: Boolean, M: Boolean)

object Producer extends DefaultJsonProtocol {

  // Define implicit JSON formatter for TradeData
  implicit val tradeDataFormat = jsonFormat9(TradeData)

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    implicit val materializer: Materializer = Materializer(system)

    val helloRoute =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    val bindingFuture = Http().bindAndHandle(helloRoute, "localhost", 8080)
    println("HTTP server running at http://localhost:8080/")

    val binanceUri = "wss://stream.binance.com:9443/ws/btcusdt@trade"

    val wsFlow: Flow[Message, Message, _] = Flow[Message]
      .collect {
        case TextMessage.Strict(text) =>
          println(s"Received raw message: $text")  // Log the raw JSON for inspection
          try {
            // Try parsing as TradeData first
            val data = text.parseJson.convertTo[TradeData]
            println(s"Parsed Binance data: Event: ${data.e}, Symbol: ${data.s}, Price: ${data.p}, Quantity: ${data.q}, Time: ${data.E}")
            TextMessage(text)
          } catch {
            case _: DeserializationException =>
              // Handle messages that cannot be parsed as TradeData
              val json = text.parseJson
              if (json.asJsObject.fields.contains("result") && json.asJsObject.fields("result") == JsNull) {
                println("Received message with result null; skipping...")
                TextMessage("")  // Skip this message
              } else if (json.asJsObject.fields.contains("error")) {
                println(s"Received error message: ${json.asJsObject.fields("error")}; skipping...")
                TextMessage("")  // Skip this error message
              } else {
                println(s"Skipping message due to deserialization error: ${text}")
                TextMessage("")  // Respond with an empty message if needed
              }
          }
      }
      .prepend(Source.single(TextMessage(
        """
          |{
          | "method": "SUBSCRIBE",
          | "params": ["btcusdt@trade"],
          | "id": 1
          |}
        """.stripMargin
      )))

    val upgradeResponse: Future[WebSocketUpgradeResponse] =
      Http().singleWebSocketRequest(WebSocketRequest(binanceUri), wsFlow)._1

    upgradeResponse.onComplete {
      case Success(upgrade) =>
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          println("Connected to WebSocket successfully")
        } else {
          println(s"Connection failed: ${upgrade.response.status}")
          system.terminate()
        }

      case Failure(ex) =>
        println(s"Connection failed with exception: ${ex.getMessage}")
        system.terminate()
    }

    println("Press ENTER to exit")
    StdIn.readLine()
    system.terminate()
  }
}

