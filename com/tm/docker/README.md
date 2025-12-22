# Docker Services

## gRPC Load Testing Services

### Services Overview

**Netty gRPC Server:**
- Image: `com.tm.kotlin.grpc_compute_service:latest`
- Port: 50051 (gRPC)
- Metrics: 9091 (Prometheus)
- CPU: 2 cores limit
- Memory: 5GB limit

**Vert.x gRPC Server:**
- Image: `com.tm.kotlin.vertx_grpc_compute_service:latest`
- Port: 50052 (gRPC)
- Metrics: 9093 (Prometheus)
- CPU: 2 cores limit
- Memory: 5GB limit

### Build & Deploy

#### Automated Deployment (Recommended)
```bash
# Build all images and start services
cd com/tm/docker
./run.sh
```

#### Manual Deployment

**Build Netty gRPC Image:**
```bash
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_image_load
```

**Build Vert.x gRPC Image:**
```bash
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_image_load
```

**Start Services:**
```bash
cd com/tm/docker

# Start Netty service
docker compose up -d grpc_compute_netty_service

# Start Vert.x service
docker compose up -d grpc_compute_vertx_service

# Start Prometheus & Grafana
docker compose up -d prometheus grafana
```

### Verify Services

**Check Running Containers:**
```bash
docker ps | grep grpc_compute
```

**Test Netty Server:**
```bash
curl http://localhost:9091/metrics | grep grpc_compute
```

**Test Vert.x Server:**
```bash
curl http://localhost:9093/metrics | grep grpc_vertx
```

### Load Testing

**From Host Machine (Recommended):**
```bash
# Test Netty server (targets port 50051)
bazel run //com/tm/kotlin/service/grpc/netty:load_test_client

# Test Vert.x server (targets port 50052)
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_load_test_client
```

### Monitoring

**Prometheus Targets:**
```
http://localhost:9090/targets
```

**Grafana Dashboard:**
```
http://localhost:3000
Username: admin
Password: admin
```

**Metrics Endpoints:**
- Netty Server: http://localhost:9091/metrics
- Vert.x Server: http://localhost:9093/metrics
- Netty Client: http://localhost:9092/metrics (when running)
- Vert.x Client: http://localhost:9094/metrics (when running)

### Prometheus Queries

**Netty P99 Latency:**
```promql
histogram_quantile(0.99, rate(grpc_compute_request_duration_seconds_bucket{job="grpc_compute_netty"}[1m])) * 1000
```

**Vert.x P99 Latency:**
```promql
histogram_quantile(0.99, rate(grpc_vertx_compute_request_duration_seconds_bucket{job="grpc_compute_vertx"}[1m])) * 1000
```

**RPS Comparison:**
```promql
# Netty RPS
rate(grpc_compute_requests_total{job="grpc_compute_netty"}[1m])

# Vert.x RPS
rate(grpc_vertx_compute_requests_total{job="grpc_compute_vertx"}[1m])
```

### Resource Limits

Both services configured with:
```yaml
deploy:
  resources:
    limits:
      cpus: "2"
      memory: 5G
    reservations:
      cpus: "1"
      memory: 3G
```

**Java Options:**
```
JAVA_OPTS=-XX:+UseContainerSupport -XX:ActiveProcessorCount=2
```

### Troubleshooting

**Service won't start:**
```bash
# Check logs
docker logs grpc_compute_netty_service
docker logs grpc_compute_vertx_service

# Check port conflicts
lsof -ti:50051
lsof -ti:50052
```

**Port already in use:**
```bash
# Kill process on port 50051
lsof -ti:50051 | xargs kill -9

# Kill process on port 50052
lsof -ti:50052 | xargs kill -9
```

**Rebuild images:**
```bash
# Remove old images
docker rmi com.tm.kotlin.grpc_compute_service:latest
docker rmi com.tm.kotlin.vertx_grpc_compute_service:latest

# Rebuild
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_image_load
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_image_load

# Restart services
docker compose restart grpc_compute_netty_service
docker compose restart grpc_compute_vertx_service
```

**Check container resource usage:**
```bash
docker stats grpc_compute_netty_service grpc_compute_vertx_service
```

### Service Management

**Stop Services:**
```bash
docker compose stop grpc_compute_netty_service
docker compose stop grpc_compute_vertx_service
```

**Restart Services:**
```bash
docker compose restart grpc_compute_netty_service
docker compose restart grpc_compute_vertx_service
```

**View Logs:**
```bash
# Follow logs
docker compose logs -f grpc_compute_netty_service
docker compose logs -f grpc_compute_vertx_service

# Last 100 lines
docker compose logs --tail=100 grpc_compute_netty_service
docker compose logs --tail=100 grpc_compute_vertx_service
```

**Remove Services:**
```bash
docker compose down grpc_compute_netty_service
docker compose down grpc_compute_vertx_service
```

### Performance Testing Workflow

1. **Start Prometheus & Grafana:**
   ```bash
   docker compose up -d prometheus grafana
   ```

2. **Build & Start Servers:**
   ```bash
   ./run.sh
   ```

3. **Verify Prometheus scraping:**
   - Open http://localhost:9090/targets
   - Check `grpc_compute_netty` and `grpc_compute_vertx` are UP

4. **Run Load Tests:**
   ```bash
   # Terminal 1: Netty load test
   bazel run //com/tm/kotlin/service/grpc/netty:load_test_client

   # Terminal 2: Vert.x load test
   bazel run //com/tm/kotlin/service/grpc/vertx:vertx_load_test_client
   ```

5. **Monitor in Grafana:**
   - Create dashboard with queries from above
   - Compare P99 latency, RPS, thread usage

6. **Analyze Results:**
   - Check dropped requests
   - Monitor callback pool queue sizes
   - Compare resource usage in `docker stats`

### Environment Variables

Both services support:
```yaml
environment:
  - JAVA_OPTS=-XX:+UseContainerSupport -XX:ActiveProcessorCount=2
```

To override CPU count:
```yaml
environment:
  - JAVA_OPTS=-XX:+UseContainerSupport -XX:ActiveProcessorCount=4
```

### Network Configuration

All services use default Docker network. Services can reach each other by name:
- `grpc_compute_netty_service:50051`
- `grpc_compute_vertx_service:50052`
- `prometheus:9090`
- `grafana:3000`

### Related Documentation

- gRPC Service Implementation: `../kotlin/service/grpc/README.md`
- Netty Server Details: `../kotlin/service/grpc/netty/`
- Vert.x Server Details: `../kotlin/service/grpc/vertx/`
- Prometheus Queries: `../kotlin/service/grpc/netty/PROMETHEUS_QUERIES.md`
