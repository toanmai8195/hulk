package com.tm.kotlin.service.bitmap_loadtest.verticles

import com.tm.kotlin.service.bitmap_loadtest.Helper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.WorkerExecutor
import org.roaringbitmap.RoaringBitmap
import java.io.*
import javax.inject.Inject

class StreamVerticle @Inject constructor(
    registry: PrometheusMeterRegistry
) : AbstractVerticle() {

    // ===== Init sẵn =====
    private val bytesFromFile: ByteArray = Helper.readBytesFromFile()
    private lateinit var bitmap: RoaringBitmap

    private lateinit var deserializeWorker: WorkerExecutor
    private lateinit var serializeWorker: WorkerExecutor

    // ===== Metric Counter =====
    private val counterDeserialize: Counter = Counter.builder("loadtest_counter")
        .tag("flow", "stream")
        .tag("func", "deserialize")
        .register(registry)

    private val counterSerialize: Counter = Counter.builder("loadtest_counter")
        .tag("flow", "stream")
        .tag("func", "serialize")
        .register(registry)

    // ===== Metric Timer =====
    private val timerDeserialize: Timer = Timer.builder("loadtest_latency")
        .tag("flow", "stream")
        .tag("func", "deserialize")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    private val timerSerialize: Timer = Timer.builder("loadtest_latency")
        .tag("flow", "stream")
        .tag("func", "serialize")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        bitmap = bytesToBitmap_Stream(bytesFromFile)

        println("Stream test size = " + bitmap.cardinality)

        deserializeWorker = vertx.createSharedWorkerExecutor("stream-deserialize-worker", Helper.POOL_SIZE)
        serializeWorker = vertx.createSharedWorkerExecutor("stream-serialize-worker", Helper.POOL_SIZE)

        vertx.setPeriodic(500) {
            repeat(2) {
                fireOneRequest()
            }
        }

        startPromise.complete()
    }

    private fun fireOneRequest() {

        // Job 1: Deserialize bằng Stream
        deserializeWorker.executeBlocking<Unit>({ promise ->
            counterDeserialize.increment()
            bytesToBitmap_Stream(bytesFromFile)
            promise.complete()
        }, false) {}

        // Job 2: Serialize bằng Stream
        serializeWorker.executeBlocking<Unit>({ promise ->
            counterSerialize.increment()
            bitmapToBytes_Stream(bitmap)
            promise.complete()
        }, false) {}
    }

    // =========================================================
    // ❌ OPTION 2 — DataStream (SLOW PATH)
    // =========================================================

    @Throws(IOException::class)
    private fun bitmapToBytes_Stream(bitmap: RoaringBitmap): ByteArray {
        return timerSerialize.record<ByteArray> {

            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)

            bitmap.serialize(dos)
            dos.flush()

            return@record baos.toByteArray()
        }
    }

    @Throws(IOException::class)
    private fun bytesToBitmap_Stream(bytes: ByteArray): RoaringBitmap {
        return timerDeserialize.record<RoaringBitmap> {

            val bais = ByteArrayInputStream(bytes)
            val dis = DataInputStream(bais)

            val bitmap = RoaringBitmap()
            bitmap.deserialize(dis)
            return@record bitmap
        }
    }

    override fun stop(stopPromise: Promise<Void>) {
        deserializeWorker.close()
        serializeWorker.close()
        stopPromise.complete()
    }
}