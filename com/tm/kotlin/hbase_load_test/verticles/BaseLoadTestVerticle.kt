package com.tm.kotlin.hbase_load_test.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.hbase_load_test.ServiceModule
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.WorkerExecutor
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import kotlin.random.Random

/**
 * Base class for load test verticles with common functionality
 */
abstract class BaseLoadTestVerticle(
    protected val hbaseClient: IHBaseClient,
    registry: PrometheusMeterRegistry,
    private val verticleName: String
) : AbstractVerticle() {

    companion object {
        const val TARGET_RPS = 1000
        const val NUM_USERS = 1000
        const val NUM_SEGMENTS_PER_REQUEST = 50
        const val SEGMENT_PREFIX = "segment_for_load_test_"
        const val TICK_INTERVAL_MS = 100L // 10 ticks per second
        const val POOL_SIZE = 50
    }

    protected lateinit var workerExecutor: WorkerExecutor

    // Metrics
    private val requestCounter: Counter = Counter.builder("hbase_loadtest_requests_total")
        .tag("verticle", verticleName)
        .register(registry)

    private val successCounter: Counter = Counter.builder("hbase_loadtest_success_total")
        .tag("verticle", verticleName)
        .register(registry)

    private val errorCounter: Counter = Counter.builder("hbase_loadtest_errors_total")
        .tag("verticle", verticleName)
        .register(registry)

    private val latencyTimer: Timer = Timer.builder("hbase_loadtest_latency")
        .tag("verticle", verticleName)
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    private val cellsReturnedCounter: Counter = Counter.builder("hbase_loadtest_cells_returned")
        .tag("verticle", verticleName)
        .register(registry)

    // RPS tracking
    private val requestsInLastSecond = AtomicLong(0)
    private val currentRps = AtomicLong(0)

    init {
        Gauge.builder("hbase_loadtest_current_rps") { currentRps.get().toDouble() }
            .tag("verticle", verticleName)
            .register(registry)
    }

    override fun start(startPromise: Promise<Void>) {
        workerExecutor = vertx.createSharedWorkerExecutor("$verticleName-worker", POOL_SIZE)

        val requestsPerTick = TARGET_RPS / (1000 / TICK_INTERVAL_MS).toInt()

        println("[$verticleName] Starting load test: $TARGET_RPS RPS ($requestsPerTick requests per tick)")

        // Fire requests periodically
        vertx.setPeriodic(TICK_INTERVAL_MS) {
            repeat(requestsPerTick) {
                fireOneRequest()
            }
        }

        // Update RPS gauge every second
        vertx.setPeriodic(1000) {
            currentRps.set(requestsInLastSecond.getAndSet(0))
        }

        startPromise.complete()
    }

    private fun fireOneRequest() {
        requestCounter.increment()
        requestsInLastSecond.incrementAndGet()

        workerExecutor.executeBlocking<Int>({ promise ->
            try {
                val userId = selectRandomUser()
                val segments = selectSegments()
                val cellsReturned = executeHBaseGet(userId, segments)
                promise.complete(cellsReturned)
            } catch (e: Exception) {
                promise.fail(e)
            }
        }, false) { result ->
            if (result.succeeded()) {
                successCounter.increment()
                cellsReturnedCounter.increment(result.result().toDouble())
            } else {
                errorCounter.increment()
            }
        }
    }

    private fun executeHBaseGet(userId: Int, segments: List<String>): Int {
        val rowKey = generateRowKey(userId)
        val columnFamily = Bytes.toBytes(ServiceModule.COLUMN_FAMILY)

        val get = Get(Bytes.toBytes(rowKey))

        // Dùng addColumn() trực tiếp thay vì filter - nhanh hơn
        segments.forEach { segment ->
            get.addColumn(columnFamily, Bytes.toBytes(segment))
        }

        val result = latencyTimer.record(Supplier {
            hbaseClient.get(get)
        })

        return result?.rawCells()?.size ?: 0
    }

    /**
     * Select a random user from 1 to NUM_USERS
     */
    private fun selectRandomUser(): Int {
        return Random.nextInt(1, NUM_USERS + 1)
    }

    /**
     * Generate row key: 10-digit number, reversed to avoid hotspot
     */
    private fun generateRowKey(userId: Int): String {
        val paddedId = userId.toString().padStart(10, '0')
        return paddedId.reversed()
    }

    /**
     * Format segment ID: segment_for_load_test_XXXX
     */
    protected fun formatSegmentId(segmentNum: Int): String {
        return "$SEGMENT_PREFIX${segmentNum.toString().padStart(4, '0')}"
    }

    /**
     * Abstract method for subclasses to implement their segment selection strategy
     */
    protected abstract fun selectSegments(): List<String>

    override fun stop(stopPromise: Promise<Void>) {
        workerExecutor.close()
        stopPromise.complete()
    }
}
