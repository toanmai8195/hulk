package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class PublishSubscribeConsumerVerticle(private val subscriberName: String) : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(PublishSubscribeConsumerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val exchangeName = "pub_sub_exchange"

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

            // Declare fanout exchange
            channel!!.exchangeDeclare(exchangeName, "fanout", true)

            // Declare a temporary exclusive queue for this subscriber
            val queueName = channel!!.queueDeclare("", false, false, true, null).queue

            // Bind queue to exchange
            channel!!.queueBind(queueName, exchangeName, "")

            logger.info("[$subscriberName] Waiting for messages on queue: $queueName")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val message = String(delivery.body, Charsets.UTF_8)
                    logger.info("[$subscriberName] Received: '$message'")

                    // Simulate processing time
                    kotlinx.coroutines.delay(1000)

                    logger.info("[$subscriberName] Processed: '$message'")
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[$subscriberName] Consumer was cancelled: $consumerTag")
            }

            channel!!.basicConsume(queueName, true, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start $subscriberName", e)
            throw e
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[$subscriberName] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping $subscriberName", e)
        }
    }
}