# Quick Start Guide: Netty vs Vert.x Load Testing

## Goal
Compare performance between Netty and Vert.x gRPC implementations under load.

## Prerequisites
```bash
# Verify Bazel installed
bazel version

# Verify Docker running
docker ps
```

## Option 1: Docker Deployment (Recommended for Production-like Testing)

### Step 1: Build & Deploy Services
```bash
cd com/tm/docker
./run.sh
```

This will:
- Build Netty gRPC image
- Build Vert.x gRPC image
- Start both services in Docker
- Start Prometheus & Grafana

### Step 2: Verify Services Running
```bash
# Check containers
docker ps | grep grpc_compute

# Test Netty metrics
curl http://localhost:9091/metrics | grep grpc_compute_requests_total

# Test Vert.x metrics
curl http://localhost:9093/metrics | grep grpc_vertx_compute_requests_total
```

### Step 3: Run Load Tests
```bash
# Terminal 1: Test Netty (port 50051)
bazel run //com/tm/kotlin/service/grpc/netty:load_test_client

# Terminal 2: Test Vert.x (port 50052)
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_load_test_client
```

### Step 4: Monitor Results

**Watch Client Logs:**
```
[5s] Target=800 rps | Actual=800 rps | Total=4000 | OK=3950 | ERR=0 | Dropped=50 | Inflight=1850 | SR=98.75%
```

**Prometheus Queries:**
```
http://localhost:9090
```

**P99 Latency Comparison:**
```promql
# Netty P99 (ms)
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000

# Vert.x P99 (ms)
histogram_quantile(0.99, rate(grpc_vertx_compute_request_duration_seconds_bucket[1m])) * 1000
```

**Resource Usage:**
```bash
docker stats grpc_compute_netty_service grpc_compute_vertx_service
```

---

## Option 2: Local Development (Faster Iteration)

### Step 1: Start Prometheus (Optional)
```bash
cd com/tm/docker
docker compose up -d prometheus grafana
```

### Step 2: Start Netty Server
```bash
# Terminal 1
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_service
```

Expected output:
```
🚀 gRPC Netty Server is running on port 50051...
📊 Prometheus metrics available at http://0.0.0.0:9091/metrics
⚙️  CPU cores: 10 (Apple M2 Pro)
⚙️  Mode: IO_BOUND
⚙️  Application thread pool size: 4
⚙️  NIO pool (mock HBase): 10 threads
⚙️  Callback pool: core=10, max=20, queue=100
🎯 Mock HBase I/O: 1ms latency
🎯 Estimated capacity: 10 threads / 3ms = 3330 RPS
```

### Step 3: Start Vert.x Server
```bash
# Terminal 2
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_service
```

Expected output:
```
🚀 gRPC Vert.x Server is running on port 50052...
📊 Prometheus metrics available at http://0.0.0.0:9093/metrics
⚙️  CPU cores: 10 (Apple M2 Pro)
⚙️  Mode: IO_BOUND
⚙️  Vert.x event loop pool: 10 threads
⚙️  Callback pool: core=10, max=20, queue=100
🎯 Mock I/O: 50ms latency
🎯 Estimated capacity: 10 threads / 52ms = 192 RPS
```

### Step 4: Run Load Tests
```bash
# Terminal 3: Test Netty
bazel run //com/tm/kotlin/service/grpc/netty:load_test_client

# Terminal 4: Test Vert.x
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_load_test_client
```

### Step 5: Analyze Results

**Client Console Output:**
```
[RAMP] Target RPS=800 → 80 req / 100ms
[5s] Target=800 rps | Actual=800 rps | Total=4000 | OK=4000 | ERR=0 | Dropped=0 | Inflight=120 | SR=100.00%
[10s] Target=800 rps | Actual=800 rps | Total=8000 | OK=8000 | ERR=0 | Dropped=0 | Inflight=150 | SR=100.00%
```

**Key Metrics:**
- `Actual`: Client submission rate (should match Target)
- `Dropped`: Requests dropped due to backpressure (should be 0)
- `Inflight`: Concurrent requests (low = fast server)
- `SR`: Success rate (should be 100%)

**Server Console Output:**
```
[NEW THREAD] Thread added: grpc-default-executor-0 (Total unique: 1)
[NEW CB THREAD] Thread added: pool-3-thread-1 (Total unique: 1)
[NEW IO THREAD] Thread added: pool-2-thread-1 (Total unique: 1)
[POOL STATS] Queue=0/100 | Active=8 | Pool=10 (largest=10)
```

---

## Configuration Tuning

### Adjust Server Capacity

**Netty (`com/tm/kotlin/service/grpc/netty/ComputeService.kt`):**
```kotlin
const val IO_DELAY_MS = 1L        // Lower = higher capacity
const val CORE_SIZE = 2           // Affects bizExecutor size

val callbackPool = ThreadPoolExecutor(
    10,  // Increase for higher RPS
    20,
    ...
)
```

**Vert.x (`com/tm/kotlin/service/grpc/vertx/VertxComputeService.kt`):**
```kotlin
const val IO_DELAY_MS = 50L       // Lower = higher capacity

val vertxOptions = VertxOptions()
    .setEventLoopPoolSize(10)     // Increase for more concurrency

val callbackPool = ThreadPoolExecutor(
    10,  // Increase for higher RPS
    20,
    ...
)
```

### Adjust Load Test Target

**Client (`LoadTestClient.kt` / `VertxLoadTestClient.kt`):**
```kotlin
private val stages = listOf(
    Stage(800, 30),     // 800 RPS for 30s
    Stage(800, 3000)    // Hold 800 RPS for 3000s
)

private val maxInflight = 2000  // Max concurrent requests
```

After changing config:
```bash
# Rebuild
bazel build //com/tm/kotlin/service/grpc/netty:grpc_compute_service
bazel build //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_service

# Restart servers
```

---

## Common Test Scenarios

### Scenario 1: Find Maximum RPS

**Goal:** Determine max sustainable RPS for each implementation

**Steps:**
1. Start with conservative load (e.g., 500 RPS)
2. Gradually increase in steps of 200 RPS
3. Watch for:
   - `Dropped > 0` → capacity reached
   - `Inflight → 2000` → backpressure kicking in
   - Latency spike in Prometheus

**Netty Configuration:**
```kotlin
const val IO_DELAY_MS = 1L  // Fast I/O
val callbackPool = ThreadPoolExecutor(10, 20, ...)
// Expected capacity: ~3300 RPS
```

**Vert.x Configuration:**
```kotlin
const val IO_DELAY_MS = 1L  // Fast I/O
val callbackPool = ThreadPoolExecutor(10, 20, ...)
// Expected capacity: ~3300 RPS
```

### Scenario 2: High Latency I/O Simulation

**Goal:** Compare behavior under slow I/O (e.g., slow HBase)

**Configuration:**
```kotlin
const val IO_DELAY_MS = 50L  // Simulate 50ms HBase latency
```

**Expected Results:**
- Netty: ~192 RPS capacity
- Vert.x: ~192 RPS capacity
- Both limited by I/O delay

### Scenario 3: Bursty Traffic

**Goal:** Test handling of sudden load spikes

**Client Configuration:**
```kotlin
private val stages = listOf(
    Stage(200, 10),   // Warmup
    Stage(1000, 5),   // Spike!
    Stage(500, 20),   // Stabilize
    Stage(200, 10)    // Cool down
)
```

**Metrics to Watch:**
- Callback pool queue size
- Thread pool expansion (activeCount → maxPoolSize)
- Dropped requests during spike

### Scenario 4: Resource Constraint Testing

**Goal:** Test under CPU/memory limits

**Docker Configuration:**
```yaml
deploy:
  resources:
    limits:
      cpus: "1"      # Reduce from 2
      memory: 2G     # Reduce from 5G
```

**Expected:**
- Lower max RPS
- Higher P99 latency
- More queue buildup

---

## Interpreting Results

### Healthy Server Indicators
✅ `Dropped = 0` (no backpressure)
✅ `Inflight < 500` (low queue depth)
✅ `SR = 100%` (no errors)
✅ P99 latency stable
✅ Callback pool queue = 0

### Server Overload Indicators
⚠️ `Dropped > 0` (client hitting backpressure)
⚠️ `Inflight → 2000` (approaching semaphore limit)
⚠️ `SR < 99%` (errors occurring)
⚠️ P99 latency increasing
⚠️ Callback pool queue > 50

### Example: Healthy vs Overloaded

**Healthy (500 RPS, capacity = 3300 RPS):**
```
[10s] Target=500 rps | Actual=500 rps | Dropped=0 | Inflight=150 | SR=100.00%
```

**Overloaded (1000 RPS, capacity = 800 RPS):**
```
[10s] Target=1000 rps | Actual=1000 rps | Dropped=2000 | Inflight=1950 | SR=75.00%
[POOL STATS] Queue=100/100 | Active=20 | Pool=20 (largest=20)
```

**Fix:** Increase callback pool size or reduce target RPS

---

## Prometheus Dashboard Setup

### Create Grafana Dashboard

1. Open http://localhost:3000
2. Login (admin/admin)
3. Create → Dashboard → Add Panel

**Panel 1: RPS Comparison**
```promql
# Query A: Netty RPS
rate(grpc_compute_requests_total[1m])

# Query B: Vert.x RPS
rate(grpc_vertx_compute_requests_total[1m])
```

**Panel 2: P99 Latency**
```promql
# Query A: Netty P99
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000

# Query B: Vert.x P99
histogram_quantile(0.99, rate(grpc_vertx_compute_request_duration_seconds_bucket[1m])) * 1000
```

**Panel 3: Callback Pool Queue**
```promql
# Query A: Netty queue
callback_pool_queue_size{job="grpc_compute_netty"}

# Query B: Vert.x queue
vertx_callback_pool_queue_size{job="grpc_compute_vertx"}
```

**Panel 4: Client Dropped Requests**
```promql
# Query A: Netty client
rate(grpc_client_dropped_total[1m])

# Query B: Vert.x client
rate(grpc_vertx_client_dropped_total[1m])
```

---

## Troubleshooting

### Problem: Client shows "Dropped > 0"

**Cause:** Server processing slower than client sending

**Solutions:**
1. Increase server callback pool:
   ```kotlin
   val callbackPool = ThreadPoolExecutor(20, 40, ...)
   ```
2. Reduce IO_DELAY_MS:
   ```kotlin
   const val IO_DELAY_MS = 1L
   ```
3. Reduce client target RPS:
   ```kotlin
   Stage(400, 30)  // Lower from 800
   ```

### Problem: Inflight → 2000 (semaphore limit)

**Cause:** Client generating requests faster than server can complete

**Solutions:**
1. Increase server capacity (see above)
2. Increase client semaphore:
   ```kotlin
   private val maxInflight = 5000
   ```

### Problem: "[WARN] Tick took 95ms"

**Cause:** Sending batch takes >80% of tick interval

**Solutions:**
1. Reduce batch size:
   ```kotlin
   Stage(400, 30)  // Lower RPS
   ```
2. Increase interval:
   ```kotlin
   val intervalMs = 200L  // From 100ms
   val batchSize = targetRps / 5  // From /10
   ```

### Problem: Server shows high latency but low RPS

**Cause:** Queue buildup in callback pool

**Check:**
```bash
# Watch server logs
[POOL STATS] Queue=100/100 | Active=20 | Pool=20
```

**Fix:**
```kotlin
val callbackPool = ThreadPoolExecutor(
    20,   // Increase from 10
    40,   // Increase from 20
    60L, TimeUnit.SECONDS,
    LinkedBlockingQueue(200)  // Increase from 100
)
```

---

## Next Steps

1. **Baseline Test:** Run both servers at 500 RPS, compare metrics
2. **Capacity Test:** Increase to 1000 RPS, observe behavior
3. **Tune Configuration:** Adjust thread pools based on results
4. **Production Planning:** Size resources for target RPS + 30% headroom
5. **Create Dashboards:** Build Grafana dashboards for ongoing monitoring

## References

- Main README: `README.md`
- Docker README: `../../../docker/README.md`
- Prometheus Queries: `netty/PROMETHEUS_QUERIES.md`
