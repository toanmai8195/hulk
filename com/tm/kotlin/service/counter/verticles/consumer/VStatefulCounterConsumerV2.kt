package com.tm.kotlin.service.counter.verticles.consumer

import com.tm.kotlin.service.counter.handler.StatefulCounterHandler
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
 * Stateful Counter Consumer Verticle - V2 with Handler Pattern
 * Handles generic stateful counter events only (no chat-specific logic)
 */
class VStatefulCounterConsumerV2 @Inject constructor(
    private val statefulHandler: StatefulCounterHandler<String>,
    private val consumerProperties: Properties
) : CoroutineVerticle() {

    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    override suspend fun start() {
        try {
            setupKafkaConsumer()
            startMessageConsumption()
            println("✅ Stateful Counter Consumer V2 started successfully")
        } catch (e: Exception) {
            println("❌ Failed to start Stateful Counter Consumer V2: ${e.message}")
        }
    }

    private fun setupKafkaConsumer() {
        kafkaConsumer = KafkaConsumer(consumerProperties.apply {
            setProperty("group.id", "stateful-counter-consumer-v2")
        })

        // Subscribe to generic stateful counter events only
        kafkaConsumer.subscribe(
            listOf(
                "stateful.counter.increment",
                "stateful.counter.decrement",
                "stateful.counter.reset",
                "stateful.counter.remove_item",
                "stateful.counter.batch"
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
            "stateful.counter.increment" -> handleGenericIncrement(event)
            "stateful.counter.decrement" -> handleGenericDecrement(event)
            "stateful.counter.reset" -> handleGenericReset(event)
            "stateful.counter.remove_item" -> handleGenericRemoveItem(event)
            "stateful.counter.batch" -> handleGenericBatch(event)
            else -> println("⚠️ Unhandled stateful topic: $topic")
        }
    }


    // Generic Event Handlers
    private suspend fun handleGenericIncrement(event: JsonObject) {
        val key = event.getString("key") ?: return
        val item = event.getString("item")
        val delta = event.getLong("delta", 1L)

        val newTotal = statefulHandler.increment(key, item, delta)
        val itemCount = item?.let { statefulHandler.getItemCount(key, it) }
        println("⬆️ Stateful counter $key${item?.let { ":$it" } ?: ""} +$delta → total: $newTotal, item: $itemCount")
    }

    private suspend fun handleGenericDecrement(event: JsonObject) {
        val key = event.getString("key") ?: return
        val item = event.getString("item")
        val delta = event.getLong("delta", 1L)

        val newTotal = statefulHandler.decrement(key, item, delta)
        val itemCount = item?.let { statefulHandler.getItemCount(key, it) }
        println("⬇️ Stateful counter $key${item?.let { ":$it" } ?: ""} -$delta → total: $newTotal, item: $itemCount")
    }

    private suspend fun handleGenericReset(event: JsonObject) {
        val key = event.getString("key") ?: return
        val item = event.getString("item")

        statefulHandler.reset(key, item)
        println("🔄 Stateful counter $key${item?.let { ":$it" } ?: ""} reset")
    }

    private suspend fun handleGenericRemoveItem(event: JsonObject) {
        val key = event.getString("key") ?: return
        val item = event.getString("item") ?: return

        statefulHandler.removeItem(key, item)
        println("🗑️ Removed item $item from stateful counter $key")
    }

    private suspend fun handleGenericBatch(event: JsonObject) {
        val key = event.getString("key") ?: return
        val operations = event.getJsonObject("operations") ?: return

        val itemDeltas = mutableMapOf<String, Long>()
        operations.forEach { (item, value) ->
            itemDeltas[item] = (value as Number).toLong()
        }

        val result = statefulHandler.batchIncrementItems(key, itemDeltas)
        println("📦 Batch operation on $key → success: ${result.success}, total: ${result.newTotal}")
    }

    override suspend fun stop() {
        try {
            kafkaConsumer.close()
            println("✅ Stateful Counter Consumer V2 stopped")
        } catch (e: Exception) {
            println("❌ Error stopping Stateful Counter Consumer V2: ${e.message}")
        }
    }
}