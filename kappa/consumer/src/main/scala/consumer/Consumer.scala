package consumer

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import java.time.Duration
import java.util.Properties
import scala.collection.JavaConverters._

object Consumer {
  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-group-id")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(java.util.Collections.singletonList("your-topic"))

    println("Starting Kafka Consumer...")
    try {
      while (true) {
        val records = consumer.poll(Duration.ofMillis(100))
        for (record <- records.asScala) {
          println(s"Consumed record: Key=${record.key()}, Value=${record.value()}, Partition=${record.partition()}, Offset=${record.offset()}")
        }
      }
    } finally {
      consumer.close()
    }
  }
}
