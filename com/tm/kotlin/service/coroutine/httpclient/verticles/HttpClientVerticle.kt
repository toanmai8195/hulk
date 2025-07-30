package com.tm.kotlin.service.coroutine.httpclient.verticles

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import javax.inject.Inject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch

class HttpClientVerticle @Inject constructor(
    vertx: Vertx
) : CoroutineVerticle() {
    val client = WebClient.create(vertx)!!
    override suspend fun start() {
        vertx.setPeriodic(100) {
            repeat(100) {
                launch {
                    val pair = generateRandomPair()
                    call(pair.first, pair.second)
                }
            }
        }

        println("🔥 Load test started")
    }

    private fun generateRandomPair(): Pair<String, String> {
        val ids = (0..9999).shuffled().take(2)
        return Pair(
            "0166000${"%04d".format(ids[0])}",
            "0166000${"%04d".format(ids[1])}"
        )
    }

    private suspend fun call(actor: String, partner: String) {
        val requestBody = JsonObject()
            .put("actor", actor)
            .put("partner", partner)

        try {
//            val response = client.post(1998, "localhost", "/mutual-check")
            val response = client.post(1998, "httpserver_coroutine", "/mutual-check")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .await()


            if (response.statusCode() != 200) {
                println("❌ Status: ${response.statusCode()} ErrorMessage=${response.statusMessage()}")
            } else {
                println("✅ MutualTotal: ${response.bodyAsJsonObject().getInteger("mutualTotal")}")
            }
        } catch (e: Exception) {
            println("❌ HTTP call failed: ${e.message}")
        }
    }

    override suspend fun stop() {
        println("HttpClientVerticle shutting down gracefully...")
    }
}