package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class PriorityQueueConsumerVerticle : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(PriorityQueueConsumerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val queueName = "priority_queue"

    override suspend fun start() {
        val factory = ConnectionFactory().apply {
            host = "localhost"
            port = 5672
            username = "guest"
            password = "guest"
        }

        try {
            connection = factory.newConnection()
            channel = connection!!.createChannel()

            // Declare priority queue with max priority of 10
            val args = mapOf("x-max-priority" to 10)
            channel!!.queueDeclare(queueName, false, false, false, args)

            logger.info("[Priority Queue Consumer] Waiting for messages...")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val message = String(delivery.body, Charsets.UTF_8)
                    val priority = delivery.properties?.priority ?: 0

                    logger.info("[Priority Queue Consumer] Received: '$message' with priority $priority")

                    // Simulate processing based on priority (higher priority = faster processing)
                    val processingTime = when {
                        priority >= 8 -> 500L  // High priority - fast processing
                        priority >= 5 -> 1500L // Medium priority - normal processing
                        else -> 3000L          // Low priority - slow processing
                    }

                    kotlinx.coroutines.delay(processingTime)

                    logger.info("[Priority Queue Consumer] Processed: '$message' (priority $priority) in ${processingTime}ms")
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[Priority Queue Consumer] Consumer was cancelled: $consumerTag")
            }

            channel!!.basicConsume(queueName, true, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start Priority Queue consumer", e)
            throw e
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[Priority Queue Consumer] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping Priority Queue consumer", e)
        }
    }
}