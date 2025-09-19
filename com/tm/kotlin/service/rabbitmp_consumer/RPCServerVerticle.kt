package com.tm.kotlin.service.rabbitmp_consumer

import com.rabbitmq.client.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class RPCServerVerticle : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(RPCServerVerticle::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val rpcQueueName = "rpc_queue"

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

            channel!!.queueDeclare(rpcQueueName, false, false, false, null)
            channel!!.basicQos(1) // Process one request at a time

            logger.info("[RPC Server] Awaiting RPC requests...")

            val deliverCallback = DeliverCallback { consumerTag, delivery ->
                launch(vertx.dispatcher()) {
                    val request = String(delivery.body, Charsets.UTF_8)
                    val correlationId = delivery.properties.correlationId
                    val replyTo = delivery.properties.replyTo

                    logger.info("[RPC Server] Received request: '$request' with correlationId: $correlationId")

                    try {
                        // Process the request
                        val response = processRequest(request)

                        // Send response back
                        val replyProperties = AMQP.BasicProperties.Builder()
                            .correlationId(correlationId)
                            .build()

                        channel!!.basicPublish("", replyTo, replyProperties, response.toByteArray())
                        logger.info("[RPC Server] Sent response: '$response' for correlationId: $correlationId")

                    } catch (e: Exception) {
                        logger.error("[RPC Server] Error processing request: $request", e)
                        val errorResponse = "Error: ${e.message}"
                        val replyProperties = AMQP.BasicProperties.Builder()
                            .correlationId(correlationId)
                            .build()
                        channel!!.basicPublish("", replyTo, replyProperties, errorResponse.toByteArray())
                    } finally {
                        // Acknowledge the request
                        channel!!.basicAck(delivery.envelope.deliveryTag, false)
                    }
                }
            }

            val cancelCallback = CancelCallback { consumerTag ->
                logger.info("[RPC Server] Consumer was cancelled: $consumerTag")
            }

            channel!!.basicConsume(rpcQueueName, false, deliverCallback, cancelCallback)

        } catch (e: Exception) {
            logger.error("Failed to start RPC Server", e)
            throw e
        }
    }

    private suspend fun processRequest(request: String): String {
        // Simulate processing time
        kotlinx.coroutines.delay(2000)

        return when {
            request.contains("Calculate") -> {
                "Result: 4" // Simple response for calculation
            }
            request.contains("Process data") -> {
                "Data processed successfully"
            }
            request.contains("Generate report") -> {
                "Report generated: Report_${System.currentTimeMillis()}.pdf"
            }
            else -> {
                "Processed: $request"
            }
        }
    }

    override suspend fun stop() {
        try {
            channel?.close()
            connection?.close()
            logger.info("[RPC Server] Stopped")
        } catch (e: Exception) {
            logger.error("Error stopping RPC Server", e)
        }
    }
}