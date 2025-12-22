# Prometheus Queries cho gRPC Load Test

## Vấn đề với Percentiles

Micrometer Prometheus **không export pre-computed percentiles** (quantile). Thay vào đó, nó export **histogram buckets** và Prometheus tính percentiles từ buckets bằng `histogram_quantile()`.

## Metric Names

### Server Metrics
- `grpc_compute_request_duration_seconds` - Histogram với buckets
- `grpc_compute_requests_total` - Counter tổng requests
- `grpc_compute_unique_threads_total` - Gauge số unique threads

### Client Metrics
- `grpc_client_request_duration_seconds` - Histogram với buckets
- `grpc_client_requests_total` - Counter tổng requests
- `grpc_client_current_rps` - Gauge RPS hiện tại
- `grpc_client_target_rps` - Gauge RPS mục tiêu

## Query P99 Latency

### Server P99 (Over 1 minute)
```promql
histogram_quantile(0.99,
  rate(grpc_compute_request_duration_seconds_bucket[1m])
)
```

### Server P95 (Over 1 minute)
```promql
histogram_quantile(0.95,
  rate(grpc_compute_request_duration_seconds_bucket[1m])
)
```

### Server P50 (Over 1 minute)
```promql
histogram_quantile(0.5,
  rate(grpc_compute_request_duration_seconds_bucket[1m])
)
```

### Client P99 (Over 1 minute)
```promql
histogram_quantile(0.99,
  rate(grpc_client_request_duration_seconds_bucket[1m])
)
```

## Query RPS

### Server RPS (Rate over 1 minute)
```promql
rate(grpc_compute_requests_total[1m])
```

### Client Actual RPS
```promql
grpc_client_current_rps
```

### Client Target RPS
```promql
grpc_client_target_rps
```

## Query Success Rate

### Client Success Rate (%)
```promql
rate(grpc_client_success_total[1m]) / rate(grpc_client_requests_total[1m]) * 100
```

### Client Error Rate (%)
```promql
rate(grpc_client_errors_total[1m]) / rate(grpc_client_requests_total[1m]) * 100
```

## Query Average Latency

### Server Average Latency (seconds)
```promql
rate(grpc_compute_request_duration_seconds_sum[1m]) /
rate(grpc_compute_request_duration_seconds_count[1m])
```

### Convert to milliseconds
```promql
(rate(grpc_compute_request_duration_seconds_sum[1m]) /
rate(grpc_compute_request_duration_seconds_count[1m])) * 1000
```

## Query Max Latency

### Server Max Latency (last scrape)
```promql
grpc_compute_request_duration_seconds_max
```

Convert to milliseconds:
```promql
grpc_compute_request_duration_seconds_max * 1000
```

## Query Unique Threads

### Total Unique Threads Used
```promql
grpc_compute_unique_threads_total
```

## Query Thread Pool Metrics

### Active Threads
```promql
thread_pool_active
```

### Thread Pool Size
```promql
thread_pool_size
```

### Thread Pool Queue Size
```promql
thread_pool_queue_size
```

## Grafana Panel Examples

### Panel 1: RPS Comparison
```promql
# Query A: Client Target RPS
grpc_client_target_rps

# Query B: Client Actual RPS
grpc_client_current_rps

# Query C: Server RPS
rate(grpc_compute_requests_total[1m])
```

### Panel 2: Latency Percentiles
```promql
# Query A: P99
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000

# Query B: P95
histogram_quantile(0.95, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000

# Query C: P50
histogram_quantile(0.5, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000

# Query D: Average
(rate(grpc_compute_request_duration_seconds_sum[1m]) /
rate(grpc_compute_request_duration_seconds_count[1m])) * 1000
```
**Unit:** milliseconds

### Panel 3: Client vs Server Latency P99
```promql
# Query A: Client P99
histogram_quantile(0.99, rate(grpc_client_request_duration_seconds_bucket[1m])) * 1000

# Query B: Server P99
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket[1m])) * 1000
```
**Unit:** milliseconds

### Panel 4: Success Rate
```promql
rate(grpc_client_success_total[1m]) / rate(grpc_client_requests_total[1m]) * 100
```
**Unit:** percent (0-100)

## Troubleshooting

### Lỗi: "no data" hoặc "NaN"

1. **Chưa có traffic:**
   ```bash
   # Chạy load test client
   bazel run //com/tm/kotlin/service/grpc/netty:load_test_client
   ```

2. **Time range quá ngắn:**
   - Đổi `[1m]` thành `[5m]` hoặc `[10m]`

3. **Metric chưa được scrape:**
   ```bash
   # Check targets trong Prometheus UI
   http://localhost:9090/targets

   # Verify metrics endpoint
   curl http://localhost:9091/metrics | grep grpc_compute
   curl http://localhost:9092/metrics | grep grpc_client
   ```

### Lỗi: "histogram_quantile: bucket le=\"+Inf\" missing"

Metric histogram chưa complete. Đợi thêm traffic hoặc check:
```bash
curl http://localhost:9091/metrics | grep grpc_compute_request_duration_seconds_bucket
```

### P99 quá cao hoặc không chính xác

1. **Buckets không đủ chi tiết:**
   - Thêm buckets trong `serviceLevelObjectives()`
   - Ví dụ: thêm 1ms, 2ms, 3ms buckets

2. **Rate window quá ngắn:**
   - Tăng `[1m]` lên `[5m]`

3. **CPU throttling:**
   - Check CPU usage: `top -pid $(pgrep -f grpc_compute_service)`
   - Nếu 100% → giảm target RPS hoặc giảm CPU task time

## Useful Aggregations

### P99 by Method (if có nhiều methods)
```promql
histogram_quantile(0.99,
  sum by (method, le) (rate(grpc_compute_request_duration_seconds_bucket[1m]))
) * 1000
```

### Total Requests (all time)
```promql
grpc_compute_requests_total
```

### Error Rate Over Time
```promql
rate(grpc_client_errors_total[1m])
```
