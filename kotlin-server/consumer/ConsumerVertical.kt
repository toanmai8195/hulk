package com.tm.consumer

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

class ConsumerVertical(private val topic: String, private val groupId: String, private val priority: String) :
    AbstractVerticle() {
    private val consumer: KafkaConsumer<String, String>
    private val executor = Executors.newFixedThreadPool(if (priority == "high") 5 else 3)

    init {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
            )
            put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
            )
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
            put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                if (priority == "high") "10" else "5"
            )
        }
        consumer = KafkaConsumer<String, String>(props)
    }

    override fun start(startPromise: Promise<Void?>?) {
        val startTime = System.currentTimeMillis()
        val eventBus = vertx.eventBus()
        val deliveryOptions = DeliveryOptions()
        deliveryOptions.sendTimeout = 1000L
        consumer.subscribe(listOf(topic), object : ConsumerRebalanceListener {
            override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
                consumer.commitSync()
            }

            override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
                println("Consumer listening... (Start in ${(System.currentTimeMillis() - startTime) / 1000L}s)")
                startPromise?.complete()
            }
        })

        vertx.setPeriodic(100) { id ->
            val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
                executor.submit {
                    eventBus.request<String>(
                        "${priority}_priority_worker",
                        "${record.value()}_${record.partition()}",
                        deliveryOptions
                    ) { rs ->
                        if (!rs.succeeded())
                            rs.cause().printStackTrace()
                    }
                }
            }
            consumer.commitAsync()
        }
    }

    override fun stop(stopPromise: Promise<Void?>?) {
        super.stop(stopPromise)
    }
}