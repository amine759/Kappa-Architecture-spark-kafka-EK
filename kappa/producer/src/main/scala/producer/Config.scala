
package producer
case class AppConfig(
    kafkaBootstrapServers: String,
    stream: String,
    topic: String
)

object Config {
  def load(): AppConfig = {
    AppConfig(
      kafkaBootstrapServers = "localhost:9092",
      stream = "wss://stream.binance.com:9443/ws/btcusdt@trade", // Adjust as needed
      topic="main-topic"
    )
  }
}
