package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CompetingConsumerVerticle(private val consumerId: Int) : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(CompetingConsumerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val queueName = "competing_consumers_queue"

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

            // Declare durable queue to match producer
            channel!!.queueDeclare(queueName, true, false, false, null)

            // Fair dispatch - don't give a consumer a new message until it has processed and acknowledged the previous one
            channel!!.basicQos(1)

            logger.info("[Competing Consumer $consumerId] Waiting for messages...")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val message = String(delivery.body, Charsets.UTF_8)
                    logger.info("[Competing Consumer $consumerId] Received: '$message'")

                    // Simulate processing time with random delay
                    val processingTime = (1000..3000).random()
                    kotlinx.coroutines.delay(processingTime.toLong())

                    logger.info("[Competing Consumer $consumerId] Processed: '$message' in ${processingTime}ms")

                    // Manually acknowledge the message
                    channel!!.basicAck(delivery.envelope.deliveryTag, false)
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[Competing Consumer $consumerId] Consumer was cancelled: $consumerTag")
            }

            // Manual acknowledgment
            channel!!.basicConsume(queueName, false, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start Competing Consumer $consumerId", e)
            throw e
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[Competing Consumer $consumerId] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping Competing Consumer $consumerId", e)
        }
    }
}