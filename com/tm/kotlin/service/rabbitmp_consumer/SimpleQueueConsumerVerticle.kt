package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SimpleQueueConsumerVerticle : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(SimpleQueueConsumerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val queueName = "simple_queue"

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

            channel!!.queueDeclare(queueName, false, false, false, null)

            logger.info("[SimpleQueue Consumer] Waiting for messages...")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val message = String(delivery.body, Charsets.UTF_8)
                    logger.info("[SimpleQueue Consumer] Received: '$message'")

                    // Simulate processing time
                    kotlinx.coroutines.delay(1000)

                    logger.info("[SimpleQueue Consumer] Processed: '$message'")
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[SimpleQueue Consumer] Consumer was cancelled: $consumerTag")
            }

            channel!!.basicConsume(queueName, true, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start SimpleQueue consumer", e)
            throw e
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[SimpleQueue Consumer] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping SimpleQueue consumer", e)
        }
    }
}