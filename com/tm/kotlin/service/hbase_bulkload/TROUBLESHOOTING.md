# Troubleshooting Guide

## Common Issues

### 1. NoClassDefFoundError: okhttp3/RequestBody

**Error:**
```
Exception in thread "main" java.lang.NoClassDefFoundError: okhttp3/RequestBody
    at io.minio.MinioClient$Builder.<init>(MinioClient.java:2724)
```

**Cause:** Missing OkHttp3 runtime dependency for MinIO client.

**Solution:**
Already fixed in BUILD file by adding OkHttp3 to `runtime_deps`:

```python
java_binary(
    name = "hbase_bulkload",
    main_class = "com.tm.kotlin.service.hbase_bulkload.MainKt",
    runtime_deps = [
        ":hbase_bulkload_lib",
        "@maven//:com_squareup_okhttp3_okhttp",  # ✅ Added
        "@maven//:com_squareup_okio_okio",       # ✅ Added
    ],
)
```

**Rebuild:**
```bash
# Rebuild binary
bazel build //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload

# Rebuild Docker image
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload_image_load

# Restart container
cd com/tm/docker
docker-compose restart hbase_bulkload
```

### 2. MinIO Connection Failed

**Error:**
```
⚠️  MinIO setup warning: Connection refused
```

**Solution:**
```bash
# Check MinIO is running
docker-compose ps minio

# Check logs
docker-compose logs minio

# Restart MinIO
docker-compose restart minio

# Verify MinIO is accessible
curl http://localhost:9001/minio/health/live
```

### 3. HBase Connection Failed

**Error:**
```
⚠️  HBase setup warning: Connection refused
```

**Solution:**
```bash
# Check HBase is running
docker-compose ps hbase

# Check logs
docker-compose logs hbase

# Restart HBase
docker-compose restart hbase

# Wait for HBase to be ready (takes ~30s)
sleep 30

# Verify with shell
docker exec -it com.tm.hbase hbase shell
```

### 4. Service Won't Start

**Check logs:**
```bash
docker-compose logs -f hbase_bulkload
```

**Common fixes:**
```bash
# 1. Ensure dependencies are running
docker-compose up -d minio hbase
sleep 30  # Wait for services

# 2. Restart service
docker-compose restart hbase_bulkload

# 3. Full rebuild
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload_image_load
docker-compose up -d --force-recreate hbase_bulkload
```

### 5. Port Conflicts

**Error:**
```
Error starting userland proxy: listen tcp4 0.0.0.0:9000: bind: address already in use
```

**Solution:**
MinIO uses port 9001 (not 9000) to avoid conflict with ClickHouse:
```yaml
minio:
  ports:
    - "9001:9000"  # API
    - "9002:9001"  # Console
```

Update service config:
```bash
export MINIO_ENDPOINT=http://minio:9000  # Internal Docker network
# or
export MINIO_ENDPOINT=http://localhost:9001  # From host
```

### 6. Interactive Menu Not Working in Docker

**Issue:** Can't interact with menu in detached mode.

**Solution:**
```bash
# Option 1: Run interactively
docker run -it --rm \
  --network tm_default \
  -e MINIO_ENDPOINT=http://minio:9000 \
  -e HBASE_ZOOKEEPER_QUORUM=hbase \
  com.tm.kotlin.hbase_bulkload:latest

# Option 2: Attach to running container
docker attach hbase_bulkload

# Option 3: Exec into container
docker exec -it hbase_bulkload sh
```

### 7. Segment Not Found

**Error:**
```
❌ Phase 2 failed: Object does not exist
```

**Cause:** Phase 1 not run or MinIO data cleared.

**Solution:**
```bash
# Run phases in order
# 1. Generate segments first
Enter your choice: 1

# 2. Then bulk load
Enter your choice: 2
```

### 8. HBase Table Not Found

**Error:**
```
❌ Error: TableNotFoundException
```

**Solution:**
Service auto-creates table on startup. If missing:

```bash
# Access HBase shell
docker exec -it com.tm.hbase hbase shell

# Create table manually
create 'segment_index', {NAME => 'cf', VERSIONS => 10}

# Verify
list
describe 'segment_index'
exit
```

### 9. Permission Denied on MinIO

**Error:**
```
❌ Access Denied
```

**Solution:**
Check credentials in environment:
```bash
# Default credentials
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Update if needed
docker-compose down
# Edit docker-compose.yml
docker-compose up -d
```

### 10. Out of Memory

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
Increase memory limits in docker-compose.yml:
```yaml
hbase_bulkload:
  environment:
    - JAVA_OPTS=-XX:MaxRAMPercentage=75.0 -Xms2G -Xmx3G
  deploy:
    resources:
      limits:
        memory: 4G
```

## Debugging Commands

### Check Service Health
```bash
# All services
docker-compose ps

# Specific service
docker-compose ps hbase_bulkload

# Service logs
docker-compose logs -f hbase_bulkload --tail=100
```

### Check Network
```bash
# List networks
docker network ls

# Inspect network
docker network inspect tm_default

# Test connectivity from container
docker exec hbase_bulkload ping minio
docker exec hbase_bulkload ping hbase
```

### Check Data
```bash
# MinIO objects
docker exec -it minio sh
mc ls local/hbase-bulkload/segments/

# HBase data
docker exec -it com.tm.hbase hbase shell
scan 'segment_index', {LIMIT => 10}
count 'segment_index'
```

### Clean State
```bash
# Stop all
docker-compose stop

# Remove containers
docker-compose rm -f

# Remove volumes (⚠️ deletes data)
docker volume rm tm_minio-data tm_hbase-data

# Restart fresh
docker-compose up -d
```

## Performance Issues

### Slow Bulk Load
```bash
# Increase batch size in code (default: 1000)
# Edit BulkLoadV1Verticle.kt and BulkLoadV2WithDeletesVerticle.kt
if (puts.size >= 5000) {  # Increase from 1000

# Increase resources
# Edit docker-compose.yml
deploy:
  resources:
    limits:
      cpus: "4"
      memory: 4G
```

### Slow MinIO
```bash
# Check disk I/O
docker stats minio

# Mount volume on SSD if possible
volumes:
  minio-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /path/to/ssd/minio-data
```

## Getting Help

If issue persists:
1. Check logs: `docker-compose logs hbase_bulkload`
2. Verify dependencies: `docker-compose ps`
3. Check resources: `docker stats`
4. Review this troubleshooting guide
5. Check README.md for configuration options
