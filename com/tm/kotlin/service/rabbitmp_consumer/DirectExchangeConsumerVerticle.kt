package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DirectExchangeConsumerVerticle(private val routingKey: String) : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(DirectExchangeConsumerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val exchangeName = "direct_exchange"

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

            // Declare direct exchange
            channel!!.exchangeDeclare(exchangeName, "direct", true)

            // Declare queue specific to routing key
            val queueName = "${routingKey}_queue"
            channel!!.queueDeclare(queueName, false, false, false, null)

            // Bind queue to exchange with routing key
            channel!!.queueBind(queueName, exchangeName, routingKey)

            logger.info("[DirectExchange Consumer - $routingKey] Waiting for messages with routing key '$routingKey'...")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val message = String(delivery.body, Charsets.UTF_8)
                    val messageRoutingKey = delivery.envelope.routingKey

                    logger.info("[DirectExchange Consumer - $routingKey] Received: '$message' with routing key '$messageRoutingKey'")

                    // Simulate different processing based on message level
                    val processingTime = when (routingKey) {
                        "error" -> 500L    // Urgent - process quickly
                        "warning" -> 1000L // Important - normal speed
                        "info" -> 2000L    // Informational - can be slower
                        else -> 1500L
                    }

                    kotlinx.coroutines.delay(processingTime)

                    logger.info("[DirectExchange Consumer - $routingKey] Processed: '$message' in ${processingTime}ms")
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[DirectExchange Consumer - $routingKey] Consumer was cancelled: $consumerTag")
            }

            channel!!.basicConsume(queueName, true, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start DirectExchange Consumer for '$routingKey'", e)
            throw e
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[DirectExchange Consumer - $routingKey] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping DirectExchange Consumer for '$routingKey'", e)
        }
    }
}