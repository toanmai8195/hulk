# gRPC Load Testing với Kotlin Client

## Tổng quan

Load test client bằng Kotlin với:
- **Async gRPC calls** - Không bị block bởi server latency
- **Ramping RPS** - Tăng dần từ 100 → 2000 RPS
- **Prometheus metrics** - Đẩy metrics lên Prometheus trong Docker

## Kiến trúc

```
┌─────────────────────┐         ┌─────────────────────┐
│  Load Test Client   │  gRPC   │   gRPC Server       │
│  (localhost:9092)   │────────>│   (localhost:50051) │
│                     │         │                     │
│  Metrics:           │         │   Metrics:          │
│  - Client RPS       │         │   - Server RPS      │
│  - Client latency   │         │   - Server latency  │
│  - Success rate     │         │   - Unique threads  │
└──────────┬──────────┘         └──────────┬──────────┘
           │                               │
           │ Scrape :9092                  │ Scrape :9091
           │                               │
           └───────────────┬───────────────┘
                          │
                   ┌──────▼──────┐
                   │ Prometheus  │
                   │ (port 9090) │
                   └─────────────┘
```

## Build

```bash
# Build server
bazel build //com/tm/kotlin/service/grpc/netty:grpc_compute_service

# Build client
bazel build //com/tm/kotlin/service/grpc/netty:load_test_client
```

## Chạy Load Test

### Bước 1: Start Prometheus (Docker)

```bash
cd com/tm/docker
docker-compose up -d prometheus
```

### Bước 2: Start gRPC Server

```bash
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_service
```

Server metrics: http://localhost:9091/metrics

### Bước 3: Start Load Test Client

```bash
bazel run //com/tm/kotlin/service/grpc/netty:load_test_client
```

Client metrics: http://localhost:9092/metrics

## Ramping Stages

Load test tăng dần RPS theo 10 stages, mỗi stage 30 giây:

| Stage | Target RPS | Duration |
|-------|-----------|----------|
| 1     | 200       | 30s      |
| 2     | 400       | 30s      |
| 3     | 600       | 30s      |
| 4     | 800       | 30s      |
| 5     | 1000      | 30s      |
| 6     | 1200      | 30s      |
| 7     | 1400      | 30s      |
| 8     | 1600      | 30s      |
| 9     | 1800      | 30s      |
| 10    | 2000      | 30s      |

**Tổng thời gian:** 5 phút (300s)

## Prometheus Queries

### Client Metrics

```promql
# Current RPS
grpc_client_current_rps

# Target RPS
grpc_client_target_rps

# Total requests
grpc_client_requests_total

# Success rate
rate(grpc_client_success_total[1m]) / rate(grpc_client_requests_total[1m]) * 100

# Client P99 latency (seconds)
grpc_client_request_duration{quantile="0.99"}

# Client P95 latency (seconds)
grpc_client_request_duration{quantile="0.95"}
```

### Server Metrics

```promql
# Server RPS
rate(grpc_compute_requests_total[1m])

# Server P99 latency (seconds)
grpc_compute_request_duration{quantile="0.99"}

# Unique threads used
grpc_compute_unique_threads_total
```

## Grafana Dashboard

Truy cập: http://localhost:3000

Import dashboard với queries:
- **Client RPS**: `grpc_client_current_rps` vs `grpc_client_target_rps`
- **Server RPS**: `rate(grpc_compute_requests_total[1m])`
- **Latency Comparison**: Client P99 vs Server P99
- **Success Rate**: `rate(grpc_client_success_total[1m]) / rate(grpc_client_requests_total[1m])`

## Troubleshooting

### Client không connect được server
```bash
# Check server đang chạy
lsof -i :50051

# Check logs
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_service
```

### Prometheus không scrape metrics
```bash
# Check client metrics endpoint
curl http://localhost:9092/metrics

# Check server metrics endpoint
curl http://localhost:9091/metrics

# Restart Prometheus
docker-compose restart prometheus
```

### RPS không đạt target
- **CPU bottleneck**: Check `top` - nếu CPU 100% thì đã hit limit
- **Network bottleneck**: Client và server nên chạy trên cùng machine để test
- **Server latency cao**: Check server P99 latency

## Performance Tuning

### Tăng CPU cores
Với CPU-bound workload:
```
Max RPS = CPU cores / Latency per request (giây)
```

Ví dụ: 10 cores, 5ms latency → Max 2000 RPS

### Giảm latency
Edit `ComputeService.kt`:
```kotlin
while ((System.nanoTime() - startTime) / 1_000_000 < 5) {
    // Giảm số operations để giảm latency
}
```

### Tăng concurrent connections
Edit `LoadTestClient.kt`:
```kotlin
val channel = ManagedChannelBuilder
    .forAddress(serverHost, serverPort)
    .maxInboundMessageSize(10 * 1024 * 1024)
    .usePlaintext()
    .build()
```
