package com.tm.kotlin.service.counter.verticles.consumer

import com.tm.kotlin.service.counter.handler.StatelessCounterHandler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*
import javax.inject.Inject

/**
 * Stateless Counter Consumer Verticle - V2 with Handler Pattern
 * Clean separation: Verticle handles Kafka/infrastructure, Handler contains business logic
 */
class VStatelessCounterConsumerV2 @Inject constructor(
    private val statelessHandler: StatelessCounterHandler,
    private val consumerProperties: Properties
) : CoroutineVerticle() {

    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    override suspend fun start() {
        try {
            setupKafkaConsumer()
            startMessageConsumption()
            println("✅ Stateless Counter Consumer V2 started successfully")
        } catch (e: Exception) {
            println("❌ Failed to start Stateless Counter Consumer V2: ${e.message}")
        }
    }

    private fun setupKafkaConsumer() {
        kafkaConsumer = KafkaConsumer(consumerProperties.apply {
            setProperty("group.id", "stateless-counter-consumer-v2")
        })

        // Subscribe to generic stateless counter events only
        kafkaConsumer.subscribe(
            listOf(
                "stateless.counter.increment",
                "stateless.counter.decrement",
                "stateless.counter.reset",
                "stateless.counter.batch"
            )
        )
    }

    private fun startMessageConsumption() {
        vertx.setPeriodic(1000) { _ ->
            try {
                val records: ConsumerRecords<String, String> = kafkaConsumer.poll(Duration.ofMillis(100))

                records.forEach { record ->
                    GlobalScope.launch {
                        try {
                            processMessage(record.topic(), JsonObject(record.value()))
                        } catch (e: Exception) {
                            println("❌ Error processing message from ${record.topic()}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Error consuming Kafka messages: ${e.message}")
            }
        }
    }

    private suspend fun processMessage(topic: String, event: JsonObject) {
        when (topic) {
            "stateless.counter.increment" -> handleGenericIncrement(event)
            "stateless.counter.decrement" -> handleGenericDecrement(event)
            "stateless.counter.reset" -> handleGenericReset(event)
            "stateless.counter.batch" -> handleGenericBatch(event)
            else -> println("⚠️ Unhandled stateless topic: $topic")
        }
    }

    // Generic Event Handlers

    private suspend fun handleGenericIncrement(event: JsonObject) {
        val key = event.getString("key") ?: return
        val delta = event.getLong("delta", 1L)

        val newTotal = statelessHandler.increment(key, delta)
        println("⬆️ Stateless counter $key +$delta → total: $newTotal")
    }

    private suspend fun handleGenericDecrement(event: JsonObject) {
        val key = event.getString("key") ?: return
        val delta = event.getLong("delta", 1L)

        val newTotal = statelessHandler.decrement(key, delta)
        println("⬇️ Stateless counter $key -$delta → total: $newTotal")
    }

    private suspend fun handleGenericReset(event: JsonObject) {
        val key = event.getString("key") ?: return

        statelessHandler.reset(key)
        println("🔄 Stateless counter $key reset → 0")
    }

    private suspend fun handleGenericBatch(event: JsonObject) {
        val operations = event.getJsonObject("operations") ?: return

        val operationsMap = operations.map.mapValues { (_, value) -> (value as Number).toLong() }
        val results = statelessHandler.batchIncrement(operationsMap)
        println("📦 Batch stateless operations → ${results.size} counters updated")
    }

    override suspend fun stop() {
        try {
            kafkaConsumer.close()
            println("✅ Stateless Counter Consumer V2 stopped")
        } catch (e: Exception) {
            println("❌ Error stopping Stateless Counter Consumer V2: ${e.message}")
        }
    }
}