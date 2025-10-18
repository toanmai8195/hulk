package com.tm.kotlin.kafka_flink.producer

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger

fun main() = runBlocking {
    val props = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "all")
    }

    val producer = KafkaProducer<String, String>(props)
    val topic = "word-count-topic"
    val counter = AtomicInteger(0)

    // Sample sentences for word counting
    val sentences = listOf(
        "Apache Flink is a powerful stream processing framework",
        "Kafka is a distributed streaming platform",
        "Flink processes data in real time",
        "Stream processing enables real time analytics",
        "Kafka and Flink work great together",
        "Big data processing with Apache Flink",
        "Real time data streaming with Kafka",
        "Flink provides stateful stream processing"
    )

    println("🚀 Word Count Producer started - publishing sentences to topic: $topic")

    try {
        while (true) {
            val id = counter.incrementAndGet()
            val sentence = sentences.random()

            val record = ProducerRecord(topic, id.toString(), sentence)

            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    println("[$id] ❌ Error: ${exception.message}")
                } else {
                    println("[$id] ✅ Sent: \"$sentence\" (partition ${metadata.partition()}, offset ${metadata.offset()})")
                }
            }

            delay(2000) // Send every 2 seconds
        }
    } finally {
        producer.close()
        println("Producer closed")
    }
}
