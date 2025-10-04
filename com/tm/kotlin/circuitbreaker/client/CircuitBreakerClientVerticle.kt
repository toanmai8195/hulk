package com.tm.kotlin.circuitbreaker.client

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

class CircuitBreakerClientVerticle @Inject constructor(
    vertx: Vertx
) : CoroutineVerticle() {

    private val client = WebClient.create(vertx)
    private val circuitBreaker: CircuitBreaker
    private val clientCmdId = java.util.concurrent.atomic.AtomicInteger(0)

    init {
        // Circuit breaker config:
        // - Nếu xử lý trên 5s trong 10% số request thì mở circuit
        val config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10) // Track 10 requests gần nhất
            .slowCallDurationThreshold(Duration.ofSeconds(5)) // Gọi > 5s = slow
            .slowCallRateThreshold(10.0f) // 10% slow calls -> open circuit
            .minimumNumberOfCalls(10) // Cần ít nhất 10 calls để đánh giá
            .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait 10s trước khi thử lại
            .permittedNumberOfCallsInHalfOpenState(3) // Cho 3 calls test khi half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        val registry = CircuitBreakerRegistry.of(config)
        circuitBreaker = registry.circuitBreaker("httpClient")

        // Log state changes
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                println("🔄 Circuit Breaker: ${event.stateTransition.fromState} -> ${event.stateTransition.toState}")
            }
            .onSlowCallRateExceeded { event ->
                println("⚠️  Slow call rate exceeded: ${event.slowCallRate}%")
            }
    }

    override suspend fun start() {
        // Start periodic calls to test endpoint
        vertx.setPeriodic(1000) {
            launch {
                callTestEndpoint()
            }
        }

        println("🔥 Circuit Breaker Client started")
        println("   Making requests to http://localhost:8080/test every 1s")
        println("   Circuit Breaker: Slow call > 5s, threshold 10%, window size 10")
    }

    private suspend fun callTestEndpoint() {
        val cmdId = clientCmdId.incrementAndGet()

        try {
            println("[$cmdId] Calling main endpoint /test")
            val result = circuitBreaker.executeSuspendFunction {
                val response = client.get(8080, "localhost", "/test")
                    .send()
                    .await()

                if (response.statusCode() == 200) {
                    response.bodyAsString()
                } else if (response.statusCode() == 503) {
                    println("[$cmdId] ⚠️  Server returned 503 Service Unavailable")
                    throw Exception("HTTP 503: Service Unavailable")
                } else {
                    throw Exception("HTTP ${response.statusCode()}: ${response.statusMessage()}")
                }
            }

            println("[$cmdId] ✅ Success (main): $result")

        } catch (e: CallNotPermittedException) {
            println("[$cmdId] 🔒 Circuit Breaker OPEN - switching to backup")
            callBackupEndpoint(cmdId)
        } catch (e: Exception) {
            when {
                e.message?.contains("503") == true -> {
                    println("[$cmdId] ❌ Service unavailable (503), switching to backup")
                    callBackupEndpoint(cmdId)
                }
                else -> {
                    println("[$cmdId] ❌ Request failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun callBackupEndpoint(cmdId: Int) {
        try {
            println("[$cmdId] Calling backup endpoint /backup/test")
            val response = client.get(8080, "localhost", "/backup/test")
                .send()
                .await()

            if (response.statusCode() == 200) {
                println("[$cmdId] 🔄 Success (backup): ${response.bodyAsString()}")
            } else {
                println("[$cmdId] ❌ Backup failed: HTTP ${response.statusCode()}")
            }
        } catch (e: Exception) {
            println("[$cmdId] ❌ Backup request failed: ${e.message}")
        }
    }

    override suspend fun stop() {
        println("CircuitBreakerClientVerticle shutting down...")
    }
}
