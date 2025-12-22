package com.tm.kotlin.service.grpc.netty

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.sun.net.httpserver.HttpServer
import com.tm.kotlin.service.grpc.netty.ComputeServiceImpl.Companion.MODE
import com.tm.kotlin.service.grpc.netty.ComputeServiceImpl.Companion.CORE_SIZE
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll
import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.net.InetSocketAddress
import java.util.concurrent.*

class ComputeServiceImpl(
    private val registry: PrometheusMeterRegistry,
    private val logExecutor: ExecutorService,
    private val callbackPool: ThreadPoolExecutor
) : ComputeServiceGrpc.ComputeServiceImplBase() {
    companion object {
        val MODE: WorkloadMode = WorkloadMode.IO_BOUND
        const val IO_DELAY_MS = 50L
        const val CORE_SIZE = 2
        private lateinit var requestCounter: Counter
        private lateinit var requestTimer: Timer
        private lateinit var uniqueThreadsGauge: Gauge
        private val uniqueThreads = ConcurrentHashMap.newKeySet<String>()
        private val uniqueCBThreads = ConcurrentHashMap.newKeySet<String>()
        private val uniqueIOThreads = ConcurrentHashMap.newKeySet<String>()

        // Mock HBase NIO pool: 10 threads
        val nioPool: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    }

    init {
        // RPS metric - Counter for total requests
        requestCounter = Counter.builder("grpc.compute.requests.total")
            .description("Total number of gRPC compute requests")
            .tag("service", "compute")
            .register(registry)

        // Unique threads metric - Gauge tracking total unique thread names
        uniqueThreadsGauge = Gauge.builder("grpc_compute_unique_threads_total") { uniqueThreads.size.toDouble() }
            .description("Total unique threads that processed requests")
            .tag("service", "compute")
            .register(registry)

        // Latency metric - Timer with histogram buckets for Prometheus
        // Prometheus tính P50, P95, P99 từ histogram buckets bằng histogram_quantile()
        requestTimer = Timer.builder("grpc.compute.request.duration")
            .description("gRPC compute request latency")
            .tag("method", "compute")
            .publishPercentileHistogram(true)  // Enable histogram for Prometheus
            .serviceLevelObjectives(
                java.time.Duration.ofMillis(1),
                java.time.Duration.ofMillis(2),
                java.time.Duration.ofMillis(5),
                java.time.Duration.ofMillis(10),
                java.time.Duration.ofMillis(25),
                java.time.Duration.ofMillis(50),
                java.time.Duration.ofMillis(75),
                java.time.Duration.ofMillis(100),
                java.time.Duration.ofMillis(150),
                java.time.Duration.ofMillis(200),
                java.time.Duration.ofMillis(250),
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(1000)
            )
            .register(registry)
    }

    override fun compute(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>
    ) {
        val sample = Timer.start(registry)

        val threadName = Thread.currentThread().name
        logExecutor.submit {
            if (uniqueThreads.add(threadName)) {
                // New thread detected - log asynchronously to avoid blocking
                println("[NEW THREAD] Thread added: $threadName (Total unique: ${uniqueThreads.size})")
            }
        }

        when (MODE) {
            WorkloadMode.CPU_BOUND ->
                handleCpuBound(request, responseObserver, sample)

            WorkloadMode.IO_BOUND ->
                handleIoBound(request, responseObserver, sample)
        }
    }

    private fun handleCpuBound(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>,
        sample: Timer.Sample
    ) {
        try {
            val start = System.currentTimeMillis()
            val result = cpuBoundTask(request.input)
            val elapsed = System.currentTimeMillis() - start

            responseObserver.onNext(
                ComputeResponse.newBuilder()
                    .setResult(result)
                    .setProcessingTimeMs(elapsed)
                    .build()
            )
            responseObserver.onCompleted()
            requestCounter.increment()
        } catch (e: Exception) {
            responseObserver.onError(e)
        } finally {
            sample.stop(requestTimer)
        }
    }

    /* ================= IO BOUND (ASYNC) ================= */

    private fun handleIoBound(
        request: ComputeRequest,
        responseObserver: StreamObserver<ComputeResponse>,
        sample: Timer.Sample
    ) {
        val submitTime = System.nanoTime()

        mockIO {
            try {
                val threadName = Thread.currentThread().name
                logExecutor.submit {
                    if (uniqueCBThreads.add(threadName)) {
                        // New thread detected - log asynchronously to avoid blocking
                        println("[NEW CB THREAD] Thread added: $threadName (Total unique: ${uniqueCBThreads.size})")
                    }
                }
                cpuBoundTask(request.input)

                val totalTime = (System.nanoTime() - submitTime) / 1_000_000

                responseObserver.onNext(
                    ComputeResponse.newBuilder()
                        .setResult(request.input)
                        .setProcessingTimeMs(totalTime)
                        .build()
                )
                responseObserver.onCompleted()
                requestCounter.increment()
            } catch (e: Exception) {
                responseObserver.onError(e)
            } finally {
                sample.stop(requestTimer)
            }
        }
    }

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

    /**
     * Mock HBase I/O pattern:
     * 1. Submit to NIO pool (10 threads) - simulates HBase async I/O
     * 2. After IO_DELAY_MS, callback executes on callback pool (core=4, max=8, queue=100)
     */
    private fun mockIO(callback: () -> Unit) {
        // Step 1: Submit I/O task to NIO pool (giống HBase async call)
        nioPool.schedule({
            val threadName = Thread.currentThread().name
            logExecutor.submit {
                if (uniqueIOThreads.add(threadName)) {
                    // New thread detected - log asynchronously to avoid blocking
                    println("[NEW IO THREAD] Thread added: $threadName (Total unique: ${uniqueIOThreads.size})")
                }
            }
            // Step 2: Sau khi I/O complete, submit callback vào callback pool
            callbackPool.execute {
                callback()
            }
        }, IO_DELAY_MS, TimeUnit.MILLISECONDS)
    }
}

fun main() {
    // Initialize Prometheus registry
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // Register JVM metrics (memory, GC, threads)
    JvmMemoryMetrics().bindTo(registry)
    JvmGcMetrics().bindTo(registry)
    JvmThreadMetrics().bindTo(registry)
    ProcessorMetrics().bindTo(registry)

    // Thread pool cho xử lý requests - tối ưu cho CPU-bound workload
    // Apple M2 Pro: 10 CPU cores
    // CPU-bound throughput = cores / latency = 10 / 0.005s = 2000 RPS
    // Thread pool size = 2x cores để handle overhead
    val bizExecutor = ThreadPoolExecutor(
        CORE_SIZE * 2,          // 4
        CORE_SIZE * 4,          // 8
        60, TimeUnit.SECONDS,
        LinkedBlockingQueue(5000),
        ThreadFactoryBuilder().setNameFormat("biz-%d").build(),
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    // Executor riêng cho async logging để không block request processing
    val logExecutor = Executors.newSingleThreadExecutor()

    // Callback pool cho I/O callbacks (mock HBase callback pattern)
    // Với IO_DELAY_MS=1ms, CPU task=2ms → processing time=3ms
    // Để đạt 650 RPS: 650 * 0.003s = 1.95 threads (minimum)
    // Tăng lên 10 core threads để có buffer cho queue overhead
    val callbackPool = ThreadPoolExecutor(
        10,  // corePoolSize - đủ cho 3333 RPS
        20,  // maximumPoolSize - handle burst
        60L, // keepAliveTime
        TimeUnit.SECONDS,
        LinkedBlockingQueue(100)  // queue size
    )

    // Register thread pool metrics - Main executor
    Gauge.builder("thread.pool.active", bizExecutor) { it.activeCount.toDouble() }
        .description("Active threads in the pool")
        .register(registry)

    Gauge.builder("thread.pool.size", bizExecutor) { it.poolSize.toDouble() }
        .description("Current thread pool size")
        .register(registry)

    Gauge.builder("thread.pool.queue.size", bizExecutor) { it.queue.size.toDouble() }
        .description("Thread pool queue size")
        .register(registry)

    Gauge.builder("thread.pool.completed.tasks", bizExecutor) { it.completedTaskCount.toDouble() }
        .description("Completed tasks count")
        .register(registry)

    // Register callback pool metrics
    Gauge.builder("callback.pool.active", callbackPool) { it.activeCount.toDouble() }
        .description("Active threads in callback pool")
        .register(registry)

    Gauge.builder("callback.pool.size", callbackPool) { it.poolSize.toDouble() }
        .description("Current callback pool size")
        .register(registry)

    Gauge.builder("callback.pool.queue.size", callbackPool) { it.queue.size.toDouble() }
        .description("Callback pool queue size")
        .register(registry)

    Gauge.builder("callback.pool.largest.size", callbackPool) { it.largestPoolSize.toDouble() }
        .description("Largest callback pool size reached")
        .register(registry)

    Gauge.builder("callback.pool.completed.tasks", callbackPool) { it.completedTaskCount.toDouble() }
        .description("Completed tasks in callback pool")
        .register(registry)

    // Netty Event Loop Groups
    // Boss group xử lý incoming connections
//    val bossGroup = NioEventLoopGroup(2)
    // Worker group xử lý I/O operations
//    val workerGroup = NioEventLoopGroup(8)

    val server: Server = NettyServerBuilder.forAddress(InetSocketAddress("0.0.0.0", 50051))
        .addService(ComputeServiceImpl(registry, logExecutor, callbackPool))
//        .directExecutor()
        // Sử dụng executor tùy chỉnh cho xử lý requests
        .executor(bizExecutor)
        // Cấu hình Netty event loop groups
//        .bossEventLoopGroup(bossGroup)
//        .workerEventLoopGroup(workerGroup)
//        .channelType(NioServerSocketChannel::class.java)
        // Tăng kích thước message
        .maxInboundMessageSize(10 * 1024 * 1024)
        .maxInboundMetadataSize(8 * 1024)
        // Tối ưu cho throughput cao
//        .maxConcurrentCallsPerConnection(100)
        // Keep-alive settings
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(5, TimeUnit.SECONDS)
        .permitKeepAliveTime(10, TimeUnit.SECONDS)
        .permitKeepAliveWithoutCalls(true)
        .build()

    // Start HTTP server for Prometheus metrics endpoint
    val metricsServer = HttpServer.create(InetSocketAddress(9091), 0)
    metricsServer.createContext("/metrics") { exchange ->
        val response = registry.scrape()
        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
        exchange.responseBody.use { os ->
            os.write(response.toByteArray())
        }
    }
    metricsServer.executor = null
    metricsServer.start()

    println("🚀 gRPC Netty Server is running on port 50051...")
    println("📊 Prometheus metrics available at http://0.0.0.0:9091/metrics")
    println("⚙️  CPU cores: 10 (Apple M2 Pro)")
    println("⚙️  Mode: ${MODE.name}")
    if (MODE == WorkloadMode.CPU_BOUND) {
        println("⚙️  Application thread pool size: ${bizExecutor.corePoolSize}")
        println("🎯 Target: 2000 RPS with 5ms CPU-bound task (10 cores × 200 RPS/core)")
    } else {
        Epoll.isAvailable()
        println("⚙️  Application thread pool size: ${bizExecutor.corePoolSize}")
        println("⚙️  NIO pool (mock HBase): 10 threads")
        println("⚙️  Callback pool: core=${callbackPool.corePoolSize}, max=${callbackPool.maximumPoolSize}, queue=100")
        println("🎯 Mock HBase I/O: ${ComputeServiceImpl.IO_DELAY_MS}ms latency")
        println("🎯 Estimated capacity: ${callbackPool.corePoolSize} threads / 3ms = ${callbackPool.corePoolSize * 333} RPS")
    }

    // Periodic pool stats logging
    val statsExecutor = Executors.newSingleThreadScheduledExecutor()
    statsExecutor.scheduleAtFixedRate({
        val queueSize = callbackPool.queue.size
        val activeThreads = callbackPool.activeCount
        val poolSize = callbackPool.poolSize
        val largestPoolSize = callbackPool.largestPoolSize

        if (queueSize > 25 || poolSize > 10) {
            println("[POOL STATS] Queue=$queueSize/${callbackPool.queue.remainingCapacity() + queueSize} | Active=$activeThreads | Pool=$poolSize (largest=$largestPoolSize)")
        }
    }, 5, 5, TimeUnit.SECONDS)

    server.start()

    // Thêm shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Shutting down gRPC server...")
        metricsServer.stop(0)
        server.shutdown()
        bizExecutor.shutdown()
        logExecutor.shutdown()
        callbackPool.shutdown()
        ComputeServiceImpl.nioPool.shutdown()
        statsExecutor.shutdown()
//        bossGroup.shutdownGracefully()
//        workerGroup.shutdownGracefully()
        try {
            if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                server.shutdownNow()
            }
            if (!bizExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                bizExecutor.shutdownNow()
            }
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow()
            }
            if (!callbackPool.awaitTermination(5, TimeUnit.SECONDS)) {
                callbackPool.shutdownNow()
            }
            if (!ComputeServiceImpl.nioPool.awaitTermination(5, TimeUnit.SECONDS)) {
                ComputeServiceImpl.nioPool.shutdownNow()
            }
            if (!statsExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                statsExecutor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            server.shutdownNow()
            bizExecutor.shutdownNow()
            logExecutor.shutdownNow()
            callbackPool.shutdownNow()
            ComputeServiceImpl.nioPool.shutdownNow()
            statsExecutor.shutdownNow()
        }
        println("✅ Server stopped")
    })

    server.awaitTermination()
}

/* ======================= MODE SWITCH ======================= */

enum class WorkloadMode {
    CPU_BOUND,
    IO_BOUND
}