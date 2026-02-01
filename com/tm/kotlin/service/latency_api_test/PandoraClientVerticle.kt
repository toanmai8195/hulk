package com.tm.kotlin.service.latency_api_test

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

class PandoraClientVerticle @Inject constructor(
    vertx: Vertx
) : CoroutineVerticle() {

    private val client = WebClient.create(
        vertx,
        WebClientOptions()
            .setMaxPoolSize(100)
            .setConnectTimeout(30000)
            .setIdleTimeout(30000)
            .setKeepAlive(true)
            .setSsl(true)
            .setTrustAll(true)
    )

    // Configuration
    private val targetRps = 20 // Target requests per second
    private val delayBetweenRequests = 1000L / targetRps // Milliseconds between each request
    private val statsReportInterval = 10_000L // Report stats every 10 seconds

    private val agentIds = listOf(
        46402175, 50314270, 38964311, 103063063, 41533207,
        46402175, 50314270, 5878968, 45179906, 38964311,
        37123367, 79922671, 50314270, 56788355, 50314270,
        46402175, 41533207, 103063063, 79922671, 86916437,
        36667146, 50314270, 46402175, 41533207, 103063063,
        17286936, 43896305, 46402175, 56788355, 50314270
    )

    private val authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJoZWxpb3MiLCJzdWIiOiJ0b2FuLmtoaWV1Iiwicm9sZXMiOlsiQURNSU4iXSwiYXVkIjoiQ0hBVCIsImlhdCI6MTc0OTAxMDUwMCwidGt0IjoiVVNFUiIsImp0aSI6IjIzZGQ2ZWJiLWJlM2EtNGE1YS1hYWYxLWNlYmIyOTJlOThiNiIsInN2YyI6WyIiXSwiYXBpcyI6WyIiXSwidiI6MX0.luM3lzIUQ7-PBoXNDiVIOGmLx_5_gvwr2_cwZNYq0q8"

    private val latencies = mutableListOf<Long>()
    private var successCount = 0
    private var errorCount = 0

    override suspend fun start() {
        println("🚀 Starting Pandora API Latency Test (Continuous Mode)...")
        println("=" * 80)
        println("📊 Configuration:")
        println("   - Target RPS: $targetRps")
        println("   - Delay between requests: ${delayBetweenRequests}ms")
        println("   - Stats report interval: ${statsReportInterval / 1000}s")
        println("   - Target API: https://s.mservice.io/internal/pandora/phonebook/uniqueId/generate")
        println()
        println("Press Ctrl+C to stop...")
        println("=" * 80)
        println()

        val startTime = System.currentTimeMillis()
        var lastStatsTime = startTime
        var requestCounter = 0

        // Launch stats reporter in background
        launch {
            while (true) {
                delay(statsReportInterval)
                printStats(startTime, requestCounter)
            }
        }

        // Run requests continuously
        while (true) {
            requestCounter++
            launch {
                callPandoraApi(requestCounter)
            }
            delay(delayBetweenRequests)
        }
    }

    private suspend fun callPandoraApi(requestNumber: Int) {
        val startTime = System.currentTimeMillis()
        try {
            val requestBody = JsonObject()
                .put("agentIds", JsonArray(agentIds))
                .put("userIds", JsonArray())

            val response = client.post(
                443,
                "35.197.149.223",  // Use IP address instead of hostname to avoid DNS issues
                "/internal/pandora/phonebook/uniqueId/generate"
            )
                .putHeader("Content-Type", "application/json")
                .putHeader("Authorization", "Bearer $authToken")
                .putHeader("Host", "s.mservice.io")  // Set Host header for proper routing
                .sendJsonObject(requestBody)
                .coAwait()

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            synchronized(latencies) {
                latencies.add(duration)
                successCount++
            }

            // Silent mode - only print errors, stats will be reported periodically

        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            synchronized(latencies) {
                errorCount++
            }

            println("❌ Request #$requestNumber | Duration: ${duration}ms | ERROR: ${e.message}")
        }
    }

    private fun printStats(startTime: Long, totalRequests: Int) {
        val currentTime = System.currentTimeMillis()
        val totalDuration = (currentTime - startTime) / 1000.0
        val actualRps = totalRequests / totalDuration

        println("\n" + "=" * 80)
        println("📈 Statistics Report (${System.currentTimeMillis()})")
        println("=" * 80)
        println("🚀 Target RPS: $targetRps | Actual RPS: %.2f".format(actualRps))
        println("⏱️  Running for: %.2f seconds".format(totalDuration))
        println("📊 Total Requests: $totalRequests")
        println("✅ Success: $successCount")
        println("❌ Errors: $errorCount")

        synchronized(latencies) {
            if (latencies.isNotEmpty()) {
                val sortedLatencies = latencies.sorted()
                val avgLatency = latencies.average()
                val minLatency = sortedLatencies.first()
                val maxLatency = sortedLatencies.last()
                val p50 = percentile(sortedLatencies, 50)
                val p95 = percentile(sortedLatencies, 95)
                val p99 = percentile(sortedLatencies, 99)

                println("\n📊 Latency Statistics (ms):")
                println("   - Min:     $minLatency ms")
                println("   - Max:     $maxLatency ms")
                println("   - Avg:     %.2f ms".format(avgLatency))
                println("   - P50:     $p50 ms")
                println("   - P95:     $p95 ms")
                println("   - P99:     $p99 ms")
                println("   - Sample size: ${latencies.size}")

                // Keep only last 1000 latencies to prevent memory issues
                if (latencies.size > 1000) {
                    val toRemove = latencies.size - 1000
                    repeat(toRemove) { latencies.removeAt(0) }
                }
            }
        }
        println("=" * 80)
        println()
    }

    private fun percentile(sortedList: List<Long>, percentile: Int): Long {
        if (sortedList.isEmpty()) return 0
        val index = ((percentile / 100.0) * sortedList.size).roundToInt()
        return sortedList[index.coerceIn(0, sortedList.size - 1)]
    }

    operator fun String.times(n: Int): String = this.repeat(n)
}
