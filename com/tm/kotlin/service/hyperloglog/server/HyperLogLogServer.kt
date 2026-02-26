package com.tm.kotlin.service.hyperloglog.server

import com.sun.net.httpserver.HttpServer
import com.tm.kotlin.common.hbase.HBaseClient
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("HyperLogLogServer")

fun main() {
    val grpcPort = System.getenv("GRPC_PORT")?.toIntOrNull() ?: 50051
    val metricsPort = System.getenv("METRICS_PORT")?.toIntOrNull() ?: 9091
    val flushIntervalMinutes = System.getenv("FLUSH_INTERVAL_MINUTES")?.toLongOrNull() ?: 1L

    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    setupJvmMetrics(registry)

    val store = HyperLogLogStore()
    val hbaseClient = createHBaseClient()
    val repository = HBaseRepository(hbaseClient)

    loadFromHBase(store, repository)

    setupCardinalityGauges(store, registry)

    val flushTimer = Timer.builder("hyperloglog_hbase_flush_duration_seconds")
        .description("HBase flush duration")
        .publishPercentileHistogram(true)
        .register(registry)

    val scheduler = startFlushScheduler(store, repository, flushIntervalMinutes, flushTimer)

    val grpcServer = startGrpcServer(grpcPort, store, registry)
    val metricsServer = startMetricsServer(metricsPort, registry)

    logger.info("HyperLogLog Server started")
    logger.info("gRPC server listening on port $grpcPort")
    logger.info("Metrics server listening on port $metricsPort")
    logger.info("HBase flush interval: $flushIntervalMinutes minutes")

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        flushToHBase(store, repository, flushTimer)
        scheduler.shutdown()
        grpcServer.shutdown()
        metricsServer.stop(0)
        logger.info("Server stopped")
    })

    grpcServer.awaitTermination()
}

private fun setupJvmMetrics(registry: PrometheusMeterRegistry) {
    JvmMemoryMetrics().bindTo(registry)
    JvmGcMetrics().bindTo(registry)
    JvmThreadMetrics().bindTo(registry)
    ProcessorMetrics().bindTo(registry)
}

private fun setupCardinalityGauges(store: HyperLogLogStore, registry: PrometheusMeterRegistry) {
    Gauge.builder("hyperloglog_cardinality_daily", store) { it.getCurrentDailyCardinality().toDouble() }
        .description("Current daily cardinality estimate")
        .register(registry)

    Gauge.builder("hyperloglog_cardinality_hourly", store) { it.getCurrentHourlyCardinality().toDouble() }
        .description("Current hourly cardinality estimate")
        .register(registry)
}

private fun createHBaseClient(): HBaseClient {
    val config: Configuration = HBaseConfiguration.create()
    config.set("hbase.zookeeper.quorum", System.getenv("HBASE_ZOOKEEPER_QUORUM") ?: "hbase")
    config.set("hbase.zookeeper.property.clientPort", System.getenv("HBASE_ZOOKEEPER_PORT") ?: "2182")

    val tableName = System.getenv("HBASE_TABLE") ?: "hulk:hyperloglog_tracking"
    return HBaseClient(config, tableName)
}

private fun loadFromHBase(store: HyperLogLogStore, repository: HBaseRepository) {
    logger.info("Loading HLL data from HBase...")
    try {
        val dailyBucket = store.getCurrentDailyBucket()
        val hourlyBucket = store.getCurrentHourlyBucket()

        val data = repository.loadCurrentBuckets(dailyBucket, hourlyBucket)
        data.forEach { (bucket, bytes) ->
            store.loadFromBytes(bucket, bytes)
            logger.info("Loaded bucket: $bucket, cardinality: ${store.getCardinality(bucket)}")
        }
        logger.info("Loaded ${data.size} buckets from HBase")
    } catch (e: Exception) {
        logger.warn("Failed to load from HBase, starting with empty store", e)
    }
}

private fun startFlushScheduler(
    store: HyperLogLogStore,
    repository: HBaseRepository,
    intervalMinutes: Long,
    flushTimer: Timer
): ScheduledExecutorService {
    val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "hbase-flush-scheduler").apply { isDaemon = true }
    }

    scheduler.scheduleAtFixedRate(
        { flushToHBase(store, repository, flushTimer) },
        intervalMinutes,
        intervalMinutes,
        TimeUnit.MINUTES
    )

    logger.info("Started HBase flush scheduler with interval: $intervalMinutes minutes")
    return scheduler
}

private fun flushToHBase(store: HyperLogLogStore, repository: HBaseRepository, flushTimer: Timer) {
    val startTime = System.nanoTime()
    try {
        val allData = store.getAllHllBytes()
        if (allData.isNotEmpty()) {
            repository.saveAll(allData)
            logger.info("Flushed ${allData.size} HLL buckets to HBase")
        }
    } catch (e: Exception) {
        logger.error("Failed to flush to HBase", e)
    } finally {
        val duration = System.nanoTime() - startTime
        flushTimer.record(duration, TimeUnit.NANOSECONDS)
    }
}

private fun startGrpcServer(
    port: Int,
    store: HyperLogLogStore,
    registry: PrometheusMeterRegistry
): Server {
    val service = HyperLogLogServiceImpl(store, registry)

    val server = NettyServerBuilder
        .forAddress(InetSocketAddress("0.0.0.0", port))
        .addService(service)
        .maxInboundMessageSize(10 * 1024 * 1024)
        .build()

    server.start()
    return server
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
