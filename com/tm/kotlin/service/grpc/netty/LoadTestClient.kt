package com.tm.kotlin.service.grpc.netty

import com.sun.net.httpserver.HttpServer
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class LoadTestClient(
    private val serverHost: String = "localhost",
    private val serverPort: Int = 50051,
    private val metricsPort: Int = 9092
) {

    /* =========================
       Scheduler (SINGLE THREAD)
       ========================= */
    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    /* =========================
       Metrics
       ========================= */
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val requestCounter: Counter
    private val successCounter: Counter
    private val errorCounter: Counter
    private val droppedCounter: Counter
    private val requestTimer: Timer
    private val currentRpsGauge: Gauge
    private val targetRpsGauge: Gauge
    private val inflightGauge: Gauge

    /* =========================
       State
       ========================= */
    private val totalRequests = AtomicLong(0)
    private val successRequests = AtomicLong(0)
    private val errorRequests = AtomicLong(0)
    private val droppedRequests = AtomicLong(0)
    private val attemptedRequests = AtomicLong(0)
    private val startTime = System.currentTimeMillis()

    @Volatile
    private var currentTargetRps = 0

    @Volatile
    private var lastStatsTime = System.currentTimeMillis()
    @Volatile
    private var lastAttemptedCount = 0L

    /* =========================
       Inflight limit
       ========================= */
    private val maxInflight = 2000
    private val inflightSemaphore = Semaphore(maxInflight)

    /* =========================
       Ramping stages
       ========================= */
    data class Stage(val targetRps: Int, val durationSeconds: Int)

    private val stages = listOf(
        Stage(800, 30),
        Stage(800, 3000)
//        Stage(400, 30),
//        Stage(600, 30),
//        Stage(800, 30),
//        Stage(1000, 30),   // ramp lên
//        Stage(1000, 600)   // giữ 1000 RPS trong 10 phút
    )

    /* =========================
       Tick task
       ========================= */
    private var tickTask: java.util.concurrent.ScheduledFuture<*>? = null

    init {
        requestCounter = Counter.builder("grpc_client_requests_total")
            .description("Total gRPC requests sent")
            .register(registry)

        successCounter = Counter.builder("grpc_client_success_total")
            .register(registry)

        errorCounter = Counter.builder("grpc_client_errors_total")
            .register(registry)

        droppedCounter = Counter.builder("grpc_client_dropped_total")
            .description("Requests dropped due to semaphore limit")
            .register(registry)

        requestTimer = Timer.builder("grpc_client_request_duration")
            .publishPercentileHistogram(true)
            .serviceLevelObjectives(
                java.time.Duration.ofMillis(5),
                java.time.Duration.ofMillis(10),
                java.time.Duration.ofMillis(25),
                java.time.Duration.ofMillis(50),
                java.time.Duration.ofMillis(100),
                java.time.Duration.ofMillis(250),
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(1000)
            )
            .register(registry)

        currentRpsGauge = Gauge.builder("grpc_client_current_rps") {
            calculateCurrentRps()
        }.register(registry)

        targetRpsGauge = Gauge.builder("grpc_client_target_rps") {
            currentTargetRps.toDouble()
        }.register(registry)

        inflightGauge = Gauge.builder("grpc_client_inflight") {
            (maxInflight - inflightSemaphore.availablePermits()).toDouble()
        }.register(registry)
    }

    /* =========================
       Public API
       ========================= */
    fun start() {
        startMetricsServer()

        val channel = ManagedChannelBuilder
            .forAddress(serverHost, serverPort)
            .usePlaintext()
            .build()

        val stub = ComputeServiceGrpc.newStub(channel)

        println("🚀 Load Test Client starting")
        println("📊 Metrics: http://localhost:$metricsPort/metrics")
        println("🎯 Target: $serverHost:$serverPort\n")

        scheduleRamping(stub)

        scheduler.scheduleAtFixedRate({
            printStats()
        }, 5, 5, TimeUnit.SECONDS)

        val totalDurationMs = stages.sumOf { it.durationSeconds } * 1000L
        scheduler.schedule({
            println("\n⏹ Test completed")
            printStats()
            channel.shutdown()
            scheduler.shutdown()
            System.exit(0)
        }, totalDurationMs + 5000, TimeUnit.MILLISECONDS)
    }

    /* =========================
       Ramping logic
       ========================= */
    private fun scheduleRamping(stub: ComputeServiceGrpc.ComputeServiceStub) {
        var delayMs = 0L
        var previous = 0

        stages.forEach { stage ->
            val steps = 10
            val stepDuration = stage.durationSeconds * 1000L / steps
            val delta = (stage.targetRps - previous) / steps.toDouble()

            repeat(steps) { i ->
                val rps = (previous + delta * (i + 1)).toInt()
                scheduler.schedule({
                    updateRps(stub, rps)
                }, delayMs + stepDuration * i, TimeUnit.MILLISECONDS)
            }

            delayMs += stage.durationSeconds * 1000L
            previous = stage.targetRps
        }
    }

    /* =========================
       Core: 100ms × batch
       ========================= */
    private fun updateRps(
        stub: ComputeServiceGrpc.ComputeServiceStub,
        targetRps: Int
    ) {
        currentTargetRps = targetRps
        tickTask?.cancel(false)

        if (targetRps <= 0) return

        val batchSize = targetRps / 10   // 100ms → 10 ticks / second
        val intervalMs = 100L

        tickTask = scheduler.scheduleAtFixedRate({
            val tickStart = System.nanoTime()
            repeat(batchSize) {
                sendAsyncRequest(stub)
            }
            val tickDuration = (System.nanoTime() - tickStart) / 1_000_000
            if (tickDuration > intervalMs * 0.8) {
                println("[WARN] Tick took ${tickDuration}ms (target: ${intervalMs}ms) - scheduler may lag!")
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS)

        println("[RAMP] Target RPS=$targetRps → $batchSize req / ${intervalMs}ms")
    }

    private fun sendAsyncRequest(stub: ComputeServiceGrpc.ComputeServiceStub) {
        attemptedRequests.incrementAndGet()

        if (!inflightSemaphore.tryAcquire()) {
            droppedCounter.increment()
            droppedRequests.incrementAndGet()
            return
        }

        val startNs = System.nanoTime()
        val request = ComputeRequest.newBuilder()
            .setInput(Random.nextLong(1_000_000))
            .build()

        requestCounter.increment()
        totalRequests.incrementAndGet()

        stub.compute(request, object : StreamObserver<ComputeResponse> {
            override fun onNext(value: ComputeResponse) {}

            override fun onError(t: Throwable) {
                errorCounter.increment()
                errorRequests.incrementAndGet()
                inflightSemaphore.release()
            }

            override fun onCompleted() {
                val duration = System.nanoTime() - startNs
                requestTimer.record(duration, TimeUnit.NANOSECONDS)
                successCounter.increment()
                successRequests.incrementAndGet()
                inflightSemaphore.release()
            }
        })
    }

    /* =========================
       Utils
       ========================= */
    private fun calculateCurrentRps(): Double {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        return if (elapsed > 0) totalRequests.get() / elapsed else 0.0
    }

    private fun printStats() {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val successRate =
            if (totalRequests.get() > 0)
                successRequests.get() * 100.0 / totalRequests.get()
            else 0.0

        // Calculate actual submission RPS over the last interval
        val now = System.currentTimeMillis()
        val intervalSeconds = (now - lastStatsTime) / 1000.0
        val currentAttempted = attemptedRequests.get()
        val attemptedInInterval = currentAttempted - lastAttemptedCount
        val actualSubmissionRps = if (intervalSeconds > 0) attemptedInInterval / intervalSeconds else 0.0

        lastStatsTime = now
        lastAttemptedCount = currentAttempted

        println(
            "[${elapsed}s] " +
                    "Target=${currentTargetRps} rps | " +
                    "Actual=${"%.0f".format(actualSubmissionRps)} rps | " +
                    "Total=${totalRequests.get()} | " +
                    "OK=${successRequests.get()} | " +
                    "ERR=${errorRequests.get()} | " +
                    "Dropped=${droppedRequests.get()} | " +
                    "Inflight=${maxInflight - inflightSemaphore.availablePermits()} | " +
                    "SR=${"%.2f".format(successRate)}%"
        )
    }

    private fun startMetricsServer() {
        val server = HttpServer.create(InetSocketAddress(metricsPort), 0)
        server.createContext("/metrics") { exchange ->
            val response = registry.scrape()
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
    }
}

fun main() {
    LoadTestClient().start()
    Thread.currentThread().join()
}