package consumer

case class AppConfig(
    kafkaBootstrapServers: String,
    kafkaGroupId: String,
    kafkaTopic: String,
    elasticsearchHost: String,
    elasticsearchPort: Int,
    elasticsearchScheme: String,
    elasticsearchUser: String,
    elasticsearchPassword: String,
    elasticsearchIndex: String,
    esJavaOpts: String
)

object Config {
  def load(): AppConfig = {
    AppConfig(
      kafkaBootstrapServers = "broker:9092",
      kafkaGroupId = "csv-consumer-group",
      kafkaTopic = "kafka-spark-topic",
      elasticsearchHost = "localhost",
      elasticsearchPort = 9200,
      elasticsearchScheme = "https",
      elasticsearchUser = "elastic",
      elasticsearchPassword = "rasta",
      elasticsearchIndex = "kafka-spark-index",
      esJavaOpts = "-Xms512m -Xmx512m"
    )
  }
}
