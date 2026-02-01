package com.tm.kotlin.service.latency_api_test

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val vertx = Vertx.vertx()

    try {
        val verticle = PandoraClientVerticle(vertx)
        vertx.deployVerticle(verticle).coAwait()

        println("✅ Verticle deployed successfully - Running continuously...")
        println("Press Ctrl+C to stop")

        // Keep the main thread alive indefinitely
        while (true) {
            delay(Long.MAX_VALUE)
        }

    } catch (e: Exception) {
        println("❌ Failed to deploy verticle: ${e.message}")
        e.printStackTrace()
    } finally {
        println("\n🛑 Shutting down...")
        vertx.close().coAwait()
    }
}
