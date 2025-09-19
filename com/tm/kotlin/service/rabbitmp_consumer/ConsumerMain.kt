package com.tm.kotlin.service.rabbitmp_consumer

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        println("Usage: consumer_main [simple|competing|priority|pubsub|rpc|direct]")
        return@runBlocking
    }

    val consumerType = args[0]
    val vertx = Vertx.vertx()

    when (consumerType) {
        "simple" -> {
            vertx.deployVerticle(SimpleQueueConsumerVerticle()) { result ->
                if (result.succeeded()) {
                    println("SimpleQueueConsumer Verticle deployed successfully")
                } else {
                    println("Failed to deploy SimpleQueueConsumer Verticle: ${result.cause()}")
                }
            }
        }
        "competing" -> {
            // Deploy multiple competing consumers
            repeat(3) { index ->
                vertx.deployVerticle(CompetingConsumerVerticle(index + 1)) { result ->
                    if (result.succeeded()) {
                        println("CompetingConsumer ${index + 1} Verticle deployed successfully")
                    } else {
                        println("Failed to deploy CompetingConsumer ${index + 1} Verticle: ${result.cause()}")
                    }
                }
            }
        }
        "priority" -> {
            vertx.deployVerticle(PriorityQueueConsumerVerticle()) { result ->
                if (result.succeeded()) {
                    println("PriorityQueueConsumer Verticle deployed successfully")
                } else {
                    println("Failed to deploy PriorityQueueConsumer Verticle: ${result.cause()}")
                }
            }
        }
        "pubsub" -> {
            // Deploy multiple subscribers
            val subscriberNames = listOf("Subscriber1", "Subscriber2", "Subscriber3")
            subscriberNames.forEach { name ->
                vertx.deployVerticle(PublishSubscribeConsumerVerticle(name)) { result ->
                    if (result.succeeded()) {
                        println("$name Verticle deployed successfully")
                    } else {
                        println("Failed to deploy $name Verticle: ${result.cause()}")
                    }
                }
            }
        }
        "rpc" -> {
            vertx.deployVerticle(RPCServerVerticle()) { result ->
                if (result.succeeded()) {
                    println("RPCServer Verticle deployed successfully")
                } else {
                    println("Failed to deploy RPCServer Verticle: ${result.cause()}")
                }
            }
        }
        "direct" -> {
            // Deploy consumers for different routing keys
            val routingKeys = listOf("info", "warning", "error")
            routingKeys.forEach { routingKey ->
                vertx.deployVerticle(DirectExchangeConsumerVerticle(routingKey)) { result ->
                    if (result.succeeded()) {
                        println("DirectExchange Consumer for '$routingKey' deployed successfully")
                    } else {
                        println("Failed to deploy DirectExchange Consumer for '$routingKey': ${result.cause()}")
                    }
                }
            }
        }
        else -> {
            println("Unknown consumer type: $consumerType")
            println("Available types: simple, competing, priority, pubsub, rpc, direct")
        }
    }
}