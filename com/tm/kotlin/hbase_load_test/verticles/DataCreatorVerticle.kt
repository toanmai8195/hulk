package com.tm.kotlin.hbase_load_test.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.hbase_load_test.ServiceModule
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.WorkerExecutor
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject

/**
 * Verticle 1: Creates 1000 HBase rows
 * - Row key: 10-digit number, reversed to avoid hotspots
 * - Each row has 5000 cells
 * - Cell qualifier: segment_for_load_test_0001 -> segment_for_load_test_5000
 */
class DataCreatorVerticle @Inject constructor(
    private val hbaseClient: IHBaseClient,
    registry: PrometheusMeterRegistry
) : AbstractVerticle() {

    companion object {
        const val NUM_ROWS = 1000
        const val NUM_SEGMENTS = 5000
        const val SEGMENT_PREFIX = "segment_for_load_test_"
        const val POOL_SIZE = 10
    }

    private lateinit var workerExecutor: WorkerExecutor

    // Metrics
    private val rowsCreatedCounter: Counter = Counter.builder("hbase_loadtest_rows_created")
        .tag("verticle", "data_creator")
        .register(registry)

    private val putTimer: Timer = Timer.builder("hbase_loadtest_put_latency")
        .tag("verticle", "data_creator")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        workerExecutor = vertx.createSharedWorkerExecutor("data-creator-worker", POOL_SIZE)

        println("[DataCreator] Starting data creation: $NUM_ROWS rows x $NUM_SEGMENTS segments each")

        // Create data in background
        workerExecutor.executeBlocking<Unit>({ promise ->
            createData()
            promise.complete()
        }, false) { result ->
            if (result.succeeded()) {
                println("[DataCreator] Data creation completed!")
            } else {
                println("[DataCreator] Data creation failed: ${result.cause()?.message}")
            }
        }

        startPromise.complete()
    }

    private fun createData() {
        val columnFamily = Bytes.toBytes(ServiceModule.COLUMN_FAMILY)
        val cellValue = Bytes.toBytes("1") // Simple value for each cell

        for (i in 1..NUM_ROWS) {
            val rowKey = generateRowKey(i)
            val put = Put(Bytes.toBytes(rowKey))

            // Add 5000 cells (segments)
            for (segmentNum in 1..NUM_SEGMENTS) {
                val qualifier = formatSegmentId(segmentNum)
                put.addColumn(columnFamily, Bytes.toBytes(qualifier), cellValue)
            }

            putTimer.record(Runnable {
                hbaseClient.put(put)
            })

            rowsCreatedCounter.increment()

            if (i % 100 == 0) {
                println("[DataCreator] Created $i / $NUM_ROWS rows")
            }
        }
    }

    /**
     * Generate row key: 10-digit number, reversed to avoid hotspot
     * Example: user 1 -> "0000000001" -> reversed "1000000000"
     */
    private fun generateRowKey(userId: Int): String {
        val paddedId = userId.toString().padStart(10, '0')
        return paddedId.reversed()
    }

    /**
     * Format segment ID: segment_for_load_test_0001 -> segment_for_load_test_5000
     */
    private fun formatSegmentId(segmentNum: Int): String {
        return "$SEGMENT_PREFIX${segmentNum.toString().padStart(4, '0')}"
    }

    override fun stop(stopPromise: Promise<Void>) {
        workerExecutor.close()
        stopPromise.complete()
    }
}
