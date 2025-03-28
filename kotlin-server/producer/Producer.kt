package com.tm.producer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ChatMessageProducer(private val topic: String) {
    private val producer: KafkaProducer<String, String>

    init {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
        }
        producer = KafkaProducer(props)
    }

    fun sendMessage(priority: String, message: String) {
        val key = if (priority == "high") "high" else "low"
        val record = ProducerRecord(topic, key, "[$priority] $message")
        producer.send(record) { metadata, exception ->
            if (exception == null) {
                println("Sent: ${record.value()} to ${metadata.topic()} at partition ${metadata.partition()}")
            } else {
                println("Send failed: ${exception.message}")
            }
        }
    }

    fun close() {
        producer.close()
    }
}

fun main() = runBlocking {
    val highProducer = ChatMessageProducer("high-priority-topic")
    val lowProducer = ChatMessageProducer("low-priority-topic")
    val job1 = launch {
        repeat(500) {
            highProducer.sendMessage("high", "[High] Message $it")
            delay(50)
        }
    }

    val job2 = launch {
        repeat(5000) {
            lowProducer.sendMessage("low", "[Low] Message $it")
            delay(50)
        }
    }

    job1.join()
    job2.join()

    highProducer.close()
    lowProducer.close()
}