package com.tm.kotlin.service.grpc.vertx

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.Timer as MicrometerTimer
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.*
import io.vertx.grpc.VertxServer
import io.vertx.grpc.VertxServerBuilder
import java.util.concurrent.*

/* ======================= MODE ======================= */

enum class WorkloadMode {
    CPU_BOUND,
    IO_BOUND
}

/* ======================= SERVICE ======================= */

class VertxComputeService(
    private val vertx: Vertx,
    private val registry: MeterRegistry,
    private val logExecutor: ExecutorService
) : ComputeServiceGrpc.ComputeServiceImplBase() {

    companion object {
        val MODE = WorkloadMode.IO_BOUND
        const val IO_DELAY_MS = 50L

        private val grpcThreads = ConcurrentHashMap.newKeySet<String>()
        private val workerThreads = ConcurrentHashMap.newKeySet<String>()
        private val ioEventLoopThreads = ConcurrentHashMap.newKeySet<String>()
    }

    /* ================= METRICS ================= */

    private val requestCounter =
        Counter.builder("grpc.vertx.compute.requests.total")
            .tag("service", "compute")
            .register(registry)

    private val requestTimer =
        MicrometerTimer.builder("grpc.vertx.compute.request.duration")
            .publishPercentileHistogram()
            .serviceLevelObjectives(
                java.time.Duration.ofMillis(1),
                java.time.Duration.ofMillis(2),
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

    /* ================= gRPC ENTRY ================= */

    override fun compute(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>
    ) {
        val sample = MicrometerTimer.start(registry)

        recordGrpcThread()

        when (MODE) {
            WorkloadMode.CPU_BOUND ->
                handleCpuBound(request, responseObserver, sample)

            WorkloadMode.IO_BOUND ->
                handleIoBound(request, responseObserver, sample)
        }
    }

    /* ================= CPU BOUND ================= */

    private fun handleCpuBound(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>,
        sample: MicrometerTimer.Sample
    ) {
        vertx.executeBlocking<Long>({ promise ->
            recordWorker()
            promise.complete(cpuBoundTask(request.input))
        }, false) { ar ->
            if (ar.succeeded()) {
                responseObserver.onNext(
                    ComputeResponse.newBuilder()
                        .setResult(ar.result())
                        .build()
                )
                responseObserver.onCompleted()
                requestCounter.increment()
            } else {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription(ar.cause().message)
                        .withCause(ar.cause())
                        .asRuntimeException()
                )
            }
            sample.stop(requestTimer)
        }
    }

    /* ================= IO BOUND ================= */

    private fun handleIoBound(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>,
        sample: MicrometerTimer.Sample
    ) {
        val start = System.nanoTime()

        vertx.setTimer(IO_DELAY_MS) {
            recordIoEventLoop()

            vertx.executeBlocking<Long>({ promise ->
                recordWorker()
                promise.complete(cpuBoundTask(request.input))
            }, false) { ar ->
                if (ar.succeeded()) {
                    val totalMs =
                        (System.nanoTime() - start) / 1_000_000

                    responseObserver.onNext(
                        ComputeResponse.newBuilder()
                            .setResult(ar.result())
                            .setProcessingTimeMs(totalMs)
                            .build()
                    )
                    responseObserver.onCompleted()
                    requestCounter.increment()
                } else {
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription(ar.cause().message)
                            .withCause(ar.cause())
                            .asRuntimeException()
                    )
                }
                sample.stop(requestTimer)
            }
        }
    }

    /* ================= THREAD LOG ================= */

    private fun recordGrpcThread() {
        val t = Thread.currentThread().name
        logExecutor.submit {
            if (grpcThreads.add(t)) {
                println("[gRPC THREAD] $t (total=${grpcThreads.size})")
            }
        }
    }

    private fun recordWorker() {
        val t = Thread.currentThread().name
        logExecutor.submit {
            if (workerThreads.add(t)) {
                println("[WORKER] $t (total=${workerThreads.size})")
            }
        }
    }

    private fun recordIoEventLoop() {
        val t = Thread.currentThread().name
        logExecutor.submit {
            if (ioEventLoopThreads.add(t)) {
                println("[IO EVENT LOOP] $t (total=${ioEventLoopThreads.size})")
            }
        }
    }

    /* ================= CPU TASK ================= */

    private fun cpuBoundTask(input: Long): Long {
        // CPU-bound task: tính toán để mất đúng ~5ms
        // Với 10 CPU cores: 10 / 0.005s = 2000 RPS
        val startTime = System.nanoTime()
        var result = input

        // CPU work ~5ms để đạt 2000 RPS trên 10 cores
        while ((System.nanoTime() - startTime) / 1_000_000 < 2) {
            for (i in 1..100) {
                result = (result * 31 + i) % 1000000007
                result = result xor (i.toLong() shl 3)
                result = result + (i * i).toLong()
            }
        }

        return result
    }
}

/* ======================= MAIN ======================= */

fun main() {

    /* ================= METRICS ================= */

    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    JvmMemoryMetrics().bindTo(registry)
    JvmGcMetrics().bindTo(registry)
    JvmThreadMetrics().bindTo(registry)
    ProcessorMetrics().bindTo(registry)

    /* ================= VERTX ================= */

    val vertx = Vertx.vertx(
        VertxOptions()
            .setEventLoopPoolSize(1)
            .setWorkerPoolSize(10)
    )

    val logExecutor = Executors.newSingleThreadExecutor()

    /* ================= gRPC SERVER ================= */

    val service = VertxComputeService(vertx, registry, logExecutor)

    val vertxServer: VertxServer =
        VertxServerBuilder
            .forAddress(vertx, "0.0.0.0", 50052)
            .addService(service)
            .build()

    vertxServer.start { ar ->
        if (ar.succeeded()) {
            println("✅ Vert.x gRPC server running on :50052")
        } else {
            ar.cause().printStackTrace()
        }
    }

    /* ================= METRICS HTTP ================= */

    vertx.createHttpServer()
        .requestHandler { ctx ->
            ctx.response()
                .putHeader("content-type", "text/plain")
                .end(registry.scrape())
        }
        .listen(9093)

    println("📊 Metrics on :9093/metrics")

    Runtime.getRuntime().addShutdownHook(Thread {
        vertxServer.shutdown()
        logExecutor.shutdown()
        vertx.close()
    })
}