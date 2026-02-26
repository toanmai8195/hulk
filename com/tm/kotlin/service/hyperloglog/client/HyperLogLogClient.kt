package com.tm.kotlin.service.hyperloglog.client

import com.sun.net.httpserver.HttpServer
import com.tm.kotlin.service.hyperloglog.server.AddSegmentsRequest
import com.tm.kotlin.service.hyperloglog.server.HyperLogLogServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val logger = LoggerFactory.getLogger("HyperLogLogClient")

private const val SEGMENT_POOL_SIZE = 1000
private const val SEGMENTS_PER_REQUEST = 50

fun main() {
    val serverHost = System.getenv("SERVER_HOST") ?: "localhost"
    val serverPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 50051
    val metricsPort = System.getenv("METRICS_PORT")?.toIntOrNull() ?: 9092
    val requestsPerSecond = System.getenv("REQUESTS_PER_SECOND")?.toIntOrNull() ?: 10

    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    JvmMemoryMetrics().bindTo(registry)
    JvmThreadMetrics().bindTo(registry)

    val segmentPool = (0 until SEGMENT_POOL_SIZE).map { "segment_$it" }

    val channel = ManagedChannelBuilder
        .forAddress(serverHost, serverPort)
        .usePlaintext()
        .build()

    val stub = HyperLogLogServiceGrpc.newBlockingStub(channel)

    val requestCounter = Counter.builder("hyperloglog_client_requests_total")
        .description("Total client requests")
        .register(registry)

    val successCounter = Counter.builder("hyperloglog_client_requests_success_total")
        .description("Successful client requests")
        .register(registry)

    val errorCounter = Counter.builder("hyperloglog_client_requests_error_total")
        .description("Failed client requests")
        .register(registry)

    val requestTimer = Timer.builder("hyperloglog_client_request_duration_seconds")
        .description("Client request duration")
        .publishPercentileHistogram(true)
        .register(registry)

    val intervalMs = 1000L / requestsPerSecond
    val scheduler = startRequestScheduler(
        stub, segmentPool, intervalMs,
        requestCounter, successCounter, errorCounter, requestTimer
    )

    val metricsServer = startMetricsServer(metricsPort, registry)

    logger.info("HyperLogLog Client started")
    logger.info("Server target: $serverHost:$serverPort")
    logger.info("Request rate: $requestsPerSecond requests/second (interval: ${intervalMs}ms)")
    logger.info("Segments per request: $SEGMENTS_PER_REQUEST")
    logger.info("Metrics server listening on port $metricsPort")

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        scheduler.shutdown()
        channel.shutdown()
        metricsServer.stop(0)
        logger.info("Client stopped")
    })

    Thread.currentThread().join()
}

private fun startRequestScheduler(
    stub: HyperLogLogServiceGrpc.HyperLogLogServiceBlockingStub,
    segmentPool: List<String>,
    intervalMs: Long,
    requestCounter: Counter,
    successCounter: Counter,
    errorCounter: Counter,
    requestTimer: Timer
): ScheduledExecutorService {
    val scheduler = Executors.newScheduledThreadPool(4) { r ->
        Thread(r, "client-sender").apply { isDaemon = true }
    }

    scheduler.scheduleAtFixedRate(
        {
            sendRequest(stub, segmentPool, requestCounter, successCounter, errorCounter, requestTimer)
        },
        0,
        intervalMs,
        TimeUnit.MILLISECONDS
    )

    return scheduler
}

private fun sendRequest(
    stub: HyperLogLogServiceGrpc.HyperLogLogServiceBlockingStub,
    segmentPool: List<String>,
    requestCounter: Counter,
    successCounter: Counter,
    errorCounter: Counter,
    requestTimer: Timer
) {
    val startTime = System.nanoTime()
    requestCounter.increment()

    try {
        val segments = selectRandomSegments(segmentPool, SEGMENTS_PER_REQUEST)

        val request = AddSegmentsRequest.newBuilder()
            .addAllSegments(segments)
            .build()

        val response = stub.addSegments(request)
        successCounter.increment()

        val requestCount = requestCounter.count().toLong()
        if (requestCount % 100 == 0L) {
            logger.info("Sent $requestCount requests, daily cardinality: ${response.estimatedCardinalityDaily}, " +
                "hourly cardinality: ${response.estimatedCardinalityHourly}")
        }
    } catch (e: StatusRuntimeException) {
        errorCounter.increment()
        logger.warn("Request failed: ${e.status}")
    } catch (e: Exception) {
        errorCounter.increment()
        logger.error("Request failed with exception", e)
    } finally {
        val duration = System.nanoTime() - startTime
        requestTimer.record(duration, TimeUnit.NANOSECONDS)
    }
}

private fun selectRandomSegments(pool: List<String>, count: Int): List<String> {
    val selected = mutableSetOf<String>()
    while (selected.size < count) {
        selected.add(pool[Random.nextInt(pool.size)])
    }
    return selected.toList()
}

private fun startMetricsServer(port: Int, registry: PrometheusMeterRegistry): HttpServer {
    val server = HttpServer.create(InetSocketAddress(port), 0)

    server.createContext("/metrics") { exchange ->
        val response = registry.scrape()
        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
        exchange.responseBody.use { os ->
            os.write(response.toByteArray())
        }
    }

    server.createContext("/health") { exchange ->
        val response = "OK"
        exchange.sendResponseHeaders(200, response.length.toLong())
        exchange.responseBody.use { os ->
            os.write(response.toByteArray())
        }
    }

    server.executor = Executors.newFixedThreadPool(2)
    server.start()
    return server
}
