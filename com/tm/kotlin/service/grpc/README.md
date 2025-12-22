# gRPC Load Testing: Netty vs Vert.x

This directory contains two gRPC server implementations for load testing and performance comparison.

## Implementations

### 1. Netty-based gRPC Server (`netty/`)
- **Port**: 50051
- **Metrics Port**: 9091
- **Event Loop**: Netty Shaded (default gRPC)
- **Architecture**: NIO pool (10 threads) + Callback pool (10-20 threads)

### 2. Vert.x-based gRPC Server (`vertx/`)
- **Port**: 50052
- **Metrics Port**: 9093
- **Event Loop**: Vert.x Event Loop (10 threads)
- **Architecture**: Vert.x timer + Callback pool (10-20 threads)

## Architecture Comparison

### Netty Implementation
```
Client Request → gRPC Netty Server → bizExecutor (4-8 threads)
                                   ↓
                              NIO Pool (10 threads)
                                   ↓ (IO_DELAY_MS)
                              Callback Pool (10-20 threads)
                                   ↓ (CPU task 2ms)
                              Response
```

**Thread Pools:**
- `bizExecutor`: Application thread pool for handling incoming requests
- `nioPool`: ScheduledExecutorService simulating HBase NIO threads
- `callbackPool`: ThreadPoolExecutor for processing callbacks after I/O

### Vert.x Implementation
```
Client Request → gRPC Server → Vert.x Event Loop (10 threads)
                             ↓
                        Vert.x Timer (IO_DELAY_MS)
                             ↓
                        Callback Pool (10-20 threads)
                             ↓ (CPU task 2ms)
                        Response
```

**Thread Pools:**
- `Vert.x Event Loop`: Built-in event loop (10 threads)
- `callbackPool`: ThreadPoolExecutor for processing callbacks

## Workload Modes

Both implementations support two modes:

### CPU_BOUND Mode
- Direct CPU processing without I/O simulation
- Latency: ~5ms CPU task
- Capacity: Limited by CPU cores (10 cores / 5ms = 2000 RPS)

### IO_BOUND Mode (Default)
- Simulates async I/O (like HBase)
- IO delay: 1ms (configurable via `IO_DELAY_MS`)
- CPU task: 2ms
- Total latency: ~3ms
- Capacity: Limited by callback pool size

## Performance Characteristics

### Netty Implementation
**Pros:**
- Native gRPC implementation (no extra dependencies)
- Fine-grained control over thread pools
- Mature and battle-tested

**Cons:**
- More complex thread pool management
- Requires manual NIO pool simulation

**Estimated Capacity:**
```kotlin
IO_DELAY_MS = 1ms
CPU_TASK = 2ms
Callback Pool: 10 cores
Capacity = 10 / 0.003s = 3,330 RPS
```

### Vert.x Implementation
**Pros:**
- Built-in event loop (simpler)
- Natural async/reactive patterns
- Timer-based I/O simulation (closer to real async I/O)

**Cons:**
- Extra dependency (Vert.x core)
- Slightly different threading model

**Estimated Capacity:**
```kotlin
IO_DELAY_MS = 50ms
CPU_TASK = 2ms
Callback Pool: 10 cores
Capacity = 10 / 0.052s = 192 RPS
```

## Build & Run

### Netty Server
```bash
# Build
bazel build //com/tm/kotlin/service/grpc/netty:grpc_compute_service

# Run
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_service
```

### Vert.x Server
```bash
# Build
bazel build //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_service

# Run
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_service
```

### Load Test Clients

**Netty Client:**
```bash
# Targets Netty server (port 50051)
# Metrics on port 9092
bazel run //com/tm/kotlin/service/grpc/netty:load_test_client
```

**Vert.x Client:**
```bash
# Targets Vert.x server (port 50052)
# Metrics on port 9094
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_load_test_client
```

## Metrics & Monitoring

### Prometheus Endpoints

**Netty:**
- Server metrics: http://localhost:9091/metrics
- Client metrics: http://localhost:9092/metrics

**Vert.x:**
- Server metrics: http://localhost:9093/metrics
- Client metrics: http://localhost:9094/metrics

### Key Metrics

**Server Metrics:**
- `grpc_compute_request_duration_seconds` (Netty) / `grpc_vertx_compute_request_duration_seconds` (Vert.x)
- `grpc_compute_requests_total`
- `grpc_compute_unique_threads_total`
- `callback_pool_active`, `callback_pool_size`, `callback_pool_queue_size`

**Client Metrics:**
- `grpc_client_request_duration_seconds` (Netty) / `grpc_vertx_client_request_duration_seconds` (Vert.x)
- `grpc_client_requests_total`
- `grpc_client_dropped_total`
- `grpc_client_inflight`
- `grpc_client_current_rps`, `grpc_client_target_rps`

### Prometheus Queries

**P99 Latency:**
```promql
# Netty server
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket[1m]))

# Vert.x server
histogram_quantile(0.99, rate(grpc_vertx_compute_request_duration_seconds_bucket[1m]))
```

**RPS:**
```promql
# Netty
rate(grpc_compute_requests_total[1m])

# Vert.x
rate(grpc_vertx_compute_requests_total[1m])
```

## Load Test Client Features

Both clients support:

1. **Async Fire-and-Forget**: Client sends requests without waiting for response
2. **Ramping**: Gradually increase RPS to target load
3. **Backpressure**: Semaphore-based inflight limit (default: 2000)
4. **Metrics Tracking**:
   - Actual submission RPS
   - Dropped requests
   - Inflight requests
   - Success rate

### Client Log Output
```
[RAMP] Target RPS=800 → 80 req / 100ms
[5s] Target=800 rps | Actual=800 rps | Total=4000 | OK=3950 | ERR=0 | Dropped=50 | Inflight=1850 | SR=98.75%
```

**Key Indicators:**
- `Actual=800`: Client is submitting 800 RPS ✅
- `Dropped=50`: 50 requests dropped due to semaphore limit ⚠️
- `Inflight=1850`: Near semaphore limit (2000) → server may be bottleneck
- `SR=98.75%`: 98.75% success rate

## Capacity Planning

### Formula
```
Capacity (RPS) = Thread Pool Size / Processing Time (seconds)
```

### Example Calculations

**Scenario 1: IO_DELAY=1ms, CPU=2ms**
```
Processing Time = 1ms + 2ms = 3ms = 0.003s
Callback Pool = 10 threads
Capacity = 10 / 0.003 = 3,330 RPS
```

**Scenario 2: IO_DELAY=50ms, CPU=2ms**
```
Processing Time = 50ms + 2ms = 52ms = 0.052s
Callback Pool = 10 threads
Capacity = 10 / 0.052 = 192 RPS
```

**Scenario 3: IO_DELAY=1ms, CPU=2ms, Pool=20**
```
Processing Time = 3ms = 0.003s
Callback Pool = 20 threads
Capacity = 20 / 0.003 = 6,660 RPS
```

## Troubleshooting

### Client drops requests (Dropped > 0)
**Cause:** Server processes slower than client sends
**Solutions:**
1. Increase server callback pool size
2. Reduce IO_DELAY_MS
3. Increase client semaphore limit (`maxInflight`)

### Inflight approaches maxInflight (2000)
**Cause:** Server bottleneck
**Solutions:**
1. Check server pool metrics: `callback_pool_queue_size`
2. Increase callback pool: `core=20, max=40`
3. Reduce processing time (IO_DELAY_MS or CPU task)

### Tick duration warning
```
[WARN] Tick took 95ms (target: 100ms) - scheduler may lag!
```
**Cause:** Sending batch of requests takes >80% of tick interval
**Solutions:**
1. Reduce batch size (lower target RPS)
2. Increase tick interval

### Server latency high but client RPS low
**Cause:** Client-side rate limiting
**Check:**
1. `grpc_client_dropped_total` - if high, increase semaphore
2. Tick duration warnings - reduce batch size
3. Network issues - check connection pool

## Configuration Reference

### Server Configuration

**Netty (`ComputeService.kt`):**
```kotlin
companion object {
    val MODE: WorkloadMode = WorkloadMode.IO_BOUND
    const val IO_DELAY_MS = 1L  // I/O delay simulation
    const val CORE_SIZE = 2     // bizExecutor core size multiplier
}

val bizExecutor = ThreadPoolExecutor(
    CORE_SIZE * 2,  // corePoolSize = 4
    CORE_SIZE * 4,  // maxPoolSize = 8
    60, TimeUnit.SECONDS,
    LinkedBlockingQueue(5000)
)

val callbackPool = ThreadPoolExecutor(
    10,   // corePoolSize
    20,   // maximumPoolSize
    60L, TimeUnit.SECONDS,
    LinkedBlockingQueue(100)
)
```

**Vert.x (`VertxComputeService.kt`):**
```kotlin
companion object {
    val MODE: WorkloadMode = WorkloadMode.IO_BOUND
    const val IO_DELAY_MS = 50L
}

val vertxOptions = VertxOptions()
    .setEventLoopPoolSize(10)
    .setWorkerPoolSize(20)

val callbackPool = ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    LinkedBlockingQueue(100)
)
```

### Client Configuration

**Load Test Stages:**
```kotlin
private val stages = listOf(
    Stage(800, 30),    // 800 RPS for 30 seconds
    Stage(800, 3000)   // Hold 800 RPS for 3000 seconds
)
```

**Semaphore Limit:**
```kotlin
private val maxInflight = 2000  // Max concurrent requests
```

## Best Practices

1. **Start with lower RPS**: Gradually ramp up to avoid overwhelming server
2. **Monitor dropped requests**: Indicates server capacity limit
3. **Watch callback pool queue**: High queue size = need more threads
4. **Check unique threads**: Should stabilize after warmup
5. **Use Prometheus queries**: Monitor P99, P95, average latency over time
6. **Capacity planning**: Always keep 20-30% headroom below theoretical max

## When to Use Each Implementation

### Use Netty When:
- You need fine-grained control over thread pools
- You're already using Netty in your stack
- You want to simulate complex async I/O patterns (e.g., multiple NIO pools)

### Use Vert.x When:
- You prefer reactive/async programming model
- You want simpler event loop management
- You're already using Vert.x for other services
- You need built-in timer-based async operations
