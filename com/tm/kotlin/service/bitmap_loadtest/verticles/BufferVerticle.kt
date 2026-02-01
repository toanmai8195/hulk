package com.tm.kotlin.service.bitmap_loadtest.verticles

import com.tm.kotlin.service.bitmap_loadtest.Helper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.WorkerExecutor
import org.roaringbitmap.RoaringBitmap
import java.nio.ByteBuffer
import javax.inject.Inject

class BufferVerticle @Inject constructor(
    registry: PrometheusMeterRegistry
) : AbstractVerticle() {

    // ===== Init sẵn dữ liệu =====
    private val bytesFromFile: ByteArray = Helper.readBytesFromFile()
    private lateinit var bitmap: RoaringBitmap

    private lateinit var deserializeWorker: WorkerExecutor
    private lateinit var serializeWorker: WorkerExecutor

    // ===== Metric Counter =====
    private val counterDeserialize: Counter = Counter.builder("loadtest_counter")
        .tag("flow", "buffer")
        .tag("func", "deserialize")
        .register(registry)

    private val counterSerialize: Counter = Counter.builder("loadtest_counter")
        .tag("flow", "buffer")
        .tag("func", "serialize")
        .register(registry)

    // ===== Metric Timer =====
    private val timerDeserialize: Timer = Timer.builder("loadtest_latency")
        .tag("flow", "buffer")
        .tag("func", "deserialize")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    private val timerSerialize: Timer = Timer.builder("loadtest_latency")
        .tag("flow", "buffer")
        .tag("func", "serialize")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)


    override fun start(startPromise: Promise<Void>) {
        bitmap = bytesToBitmap_Buffer(bytesFromFile)

        println("Buffer test size = " + bitmap.cardinality)

        deserializeWorker = vertx.createSharedWorkerExecutor("deserialize-worker", Helper.POOL_SIZE)
        serializeWorker = vertx.createSharedWorkerExecutor("serialize-worker", Helper.POOL_SIZE)

        vertx.setPeriodic(500) {
            repeat(2) {
                fireOneRequest()
            }
        }

        startPromise.complete()
    }

    private fun fireOneRequest() {

        // Job 1: Deserialize
        deserializeWorker.executeBlocking<Unit>({ promise ->
            counterDeserialize.increment()
            bytesToBitmap_Buffer(bytesFromFile)
            promise.complete()
        }, false) {}

        // Job 2: Serialize
        serializeWorker.executeBlocking<Unit>({ promise ->
            counterSerialize.increment()
            bitmapToBytes_Buffer(bitmap)
            promise.complete()
        }, false) {}
    }

    // =========================================================
    // ByteBuffer FAST PATH
    // =========================================================

    private fun bitmapToBytes_Buffer(bitmap: RoaringBitmap): ByteArray {
        return timerSerialize.record<ByteArray> {
            val size = bitmap.serializedSizeInBytes()
            val buffer = ByteBuffer.allocate(size)
            bitmap.serialize(buffer)
            return@record buffer.array()
        }
    }

    private fun bytesToBitmap_Buffer(bytes: ByteArray): RoaringBitmap {
        return timerDeserialize.record<RoaringBitmap> {
            val bm = RoaringBitmap()
            bm.deserialize(ByteBuffer.wrap(bytes))
            return@record bm
        }
    }

    override fun stop(stopPromise: Promise<Void>) {
        deserializeWorker.close()
        serializeWorker.close()
        stopPromise.complete()
    }
}