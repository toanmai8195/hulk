package com.tm.kotlin.service.segment_client

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val vertx = Vertx.vertx()

    try {
        val verticle = SegmentClientVerticle(vertx)
        vertx.deployVerticle(verticle).coAwait()

        println("✅ Verticle deployed successfully")

        // No need to wait, verticle.start() will complete when all requests are done

    } catch (e: Exception) {
        println("❌ Failed to deploy verticle: ${e.message}")
        e.printStackTrace()
    } finally {
        println("\n🛑 Shutting down...")
        vertx.close().coAwait()
    }
}
