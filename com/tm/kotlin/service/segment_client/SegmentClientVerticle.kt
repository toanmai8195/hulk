package com.tm.kotlin.service.segment_client

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.launch
import javax.inject.Inject

class SegmentClientVerticle @Inject constructor(
    vertx: Vertx
) : CoroutineVerticle() {

    private val client = WebClient.create(
        vertx,
        WebClientOptions()
            .setMaxPoolSize(50)
            .setConnectTimeout(1500000)
            .setIdleTimeout(3000)
            .setKeepAlive(true)
    )

    private val segments = listOf(
        "toanmai1_test_indexed",
        "toanmai_test_timeout",
        "toanmai_test_error",
        "toanmai_test_indexed_timeout",
        "toanmai_test_indexed_error",
        "toanmai_test_last_version_timeout",
        "toanmai_test_last_version_error",
        "toanmai1_test_last_version_timeout",
        "toanmai1_test_non_indexed"
//        "Test_whitelist_semtn_binhcan",
//        "Clone_of_test_05122025_3",
//        "Clone_of_Clone_of_test_05122025_3",
//        "Clone_of_20251015_test_cleanup_11",
//        "241226_ttt_plus_package_test",
//        "2601_ttt_plus_silver_b",
//        "2601_ttt_plus_gold_b",
//        "2601_ttt_plus_platinum_b"
    )

    override suspend fun start() {
        println("🚀 Starting Segment API Client...")
        println("=" * 80)
        println("📊 Testing ${segments.size} segments (all async)...")
        println()

        // Call all segments async at once
        val jobs = segments.mapIndexed { index, segment ->
            launch {
                callSegmentApi(index + 1, segment)
            }
        }

        // Wait for all requests to complete
        jobs.forEach { it.join() }
        println("\n✅ All requests completed!")
    }

    private suspend fun callSegmentApi(requestNumber: Int, segment: String) {
        val startTime = System.currentTimeMillis()
        try {
            val requestBody = JsonObject()
                .put("auth", JsonObject()
                    .put("callerId", "segment_test")
                    .put("cmdId", "100")
                    .put("accessToken", "segment_test")
                )
                .put("uid", "41812160")
                .put("uidType", "AGENT_ID")
                .put("segmentIds", JsonArray().add(segment))

            val response = client.post(
                30852,
                "inginx-dev.mservice.io",
                "/api/segment-api/filter-segments-of-user"
            )
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .coAwait()

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            // Parse uidStatus from response
            val uidStatus = try {
                val body = response.bodyAsJsonObject()
                val data = body.getJsonObject("data")
                val filterResultMap = data?.getJsonObject("filterResultMap")
                val segmentResult = filterResultMap?.getJsonObject(segment)
                segmentResult?.getString("uidStatus") ?: "UNKNOWN"
            } catch (e: Exception) {
                "PARSE_ERROR: ${e.message}"
            }

            println("Request #$requestNumber | Segment: $segment | Duration: ${duration}ms | Status: ${response.statusCode()} | uidStatus: $uidStatus")

        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("Request #$requestNumber | Segment: $segment | Duration: ${duration}ms | ERROR: ${e.message}")
        }
    }

    operator fun String.times(n: Int): String = this.repeat(n)
}
