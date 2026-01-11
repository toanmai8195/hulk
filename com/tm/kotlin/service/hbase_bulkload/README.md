# HBase Bulk Load - Segment Rebuild Service

## Overview

Service thử nghiệm **inverted index segment rebuild** sử dụng:
- **RoaringBitmap**: Compressed bitmap cho user segments
- **MinIO**: Object storage cho raw segment data
- **HBase**: Inverted index storage với bulk load
- **DeleteColumn**: Xóa old segment versions khi rebuild

## Purpose

Mô phỏng rebuild inverted index:
- Mỗi user có thể thuộc nhiều segments
- Segments được versioned và cần update
- Xóa old segment memberships khi users không còn thuộc segment đó

## Data Flow

```
Phase 1: Generate Segments
  ├─ Segment V1: users 1-1,000,000 → MinIO
  └─ Segment V2: users 100,001-1,100,000 → MinIO

Phase 2: Bulk Load V1
  └─ 1M rows (user_id → segment_v1) → HBase

Phase 3: Bulk Load V2 + Deletes
  ├─ Put 1M rows (user_id → segment_v2)
  └─ Delete 100K rows (users 1-100,000 không còn segment_v1)

Phase 4: Query & Verify
  ├─ Users 1-100,000: EMPTY ✅
  └─ Users 100,001-1,100,000: segment_v2 ✅
```

## Quick Start

### Build OCI Image
```bash
# Build và load Docker image với Bazel OCI (1 command)
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload_image_load

# Verify image
docker images | grep hbase_bulkload
# Output: com.tm.kotlin.hbase_bulkload:latest (530MB)
```

### Deploy với Docker Compose
```bash
cd com/tm/docker

# Start tất cả services
docker-compose up -d minio hbase hbase_bulkload

# View logs
docker-compose logs -f hbase_bulkload
```

### Service Endpoints
- **MinIO API**: http://localhost:9001
- **MinIO Console**: http://localhost:9002 (minioadmin/minioadmin)
- **HBase Master UI**: http://localhost:16010

## Interactive Console Menu

Service hiển thị menu cho phép chọn phase muốn chạy:

```
======================================================================
MENU - Select Phase to Run
======================================================================
1. Phase 1 - Generate Segments (Save to MinIO)
2. Phase 2 - Bulk Load Segment V1 to HBase
3. Phase 3 - Bulk Load Segment V2 with Deletes
4. Phase 4 - Query and Verify Results
5. Run All Phases (Auto Mode)
0. Exit
======================================================================
Enter your choice:
```

### Usage Examples

**Run All Phases (Recommended for first time)**
```
Enter your choice: 5

✅ Phase 1: Generate Segments → MinIO
✅ Phase 2: Bulk Load V1 → HBase (1M rows)
✅ Phase 3: Bulk Load V2 + Deletes (1M puts, 100K deletes)
✅ Phase 4: Query & Verify → All tests passed!
```

**Run Individual Phases**
```
# Regenerate segments
Enter your choice: 1
✅ Phase 1 completed!

# Query current data
Enter your choice: 4
📊 Test Results: ...
```

**Expected Results (Phase 4)**
```
======================================================================
Phase 4: Query and Verify Results
======================================================================
📊 Querying user segments:
================================================================================
✅ User 1: EMPTY
✅ User 50,000: EMPTY
✅ User 100,000: EMPTY
✅ User 100,001: V2
✅ User 500,000: V2
✅ User 1,000,000: V2
✅ User 1,100,000: V2
================================================================================

📈 Test Results:
  ✅ Success: 7
  ✅ All tests passed! Inverted index segment rebuild works correctly.
```

## Verify Results

### Query HBase
```bash
docker exec -it com.tm.hbase hbase shell

# Test queries
get 'segment_index', 'user_0000100001'  # Should have segment_v2
get 'segment_index', 'user_0000050000'  # Should be empty

scan 'segment_index', {LIMIT => 10}
exit
```

### Check MinIO
Access console at http://localhost:9002
- Bucket: `hbase-bulkload`
- Files: `segments/segment_v1.bin`, `segments/segment_v2.bin`

## Components

### 1. SegmentGeneratorVerticle
Generates segments với RoaringBitmap và lưu vào MinIO:
- Segment V1: 1M users (1-1,000,000)
- Segment V2: 1M users (100,001-1,100,000)

### 2. BulkLoadV1Verticle
Reads Segment V1 từ MinIO, tạo inverted index và load vào HBase:
- Inverted index: `user_id → segment_v1`
- Batch puts: 1000 rows/batch

### 3. BulkLoadV2WithDeletesVerticle
Processes V2 và xóa users không còn trong segment:
- Put all users in V2 with `segment_v2`
- Delete `segment_v1` column cho users 1-100,000

### 4. SegmentQueryVerticle
Queries và verifies kết quả:
- Users 1-100,000: Empty (deleted)
- Users 100,001-1,100,000: Have segment_v2

## Configuration

Environment variables (trong docker-compose.yml):

```yaml
environment:
  - MINIO_ENDPOINT=http://minio:9000
  - MINIO_BUCKET=hbase-bulkload
  - MINIO_ACCESS_KEY=minioadmin
  - MINIO_SECRET_KEY=minioadmin
  - HBASE_TABLE=segment_index
  - HBASE_COLUMN_FAMILY=cf
  - HBASE_ZOOKEEPER_QUORUM=hbase
  - HBASE_ZOOKEEPER_PORT=2181
```

## Local Development

### Build Binary
```bash
bazel build //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload
```

### Run Locally (Interactive Mode)
```bash
# Start MinIO
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"

# Start HBase
docker run -d -p 2181:2181 -p 16010:16010 \
  --name hbase harisekhon/hbase

# Run service (interactive menu will appear)
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload

# Menu example:
# 1. Phase 1 - Generate Segments
# 2. Phase 2 - Bulk Load V1
# 3. Phase 3 - Bulk Load V2 with Deletes
# 4. Phase 4 - Query and Verify
# 5. Run All Phases
# 0. Exit
```

### Run in Docker (Interactive Mode)
```bash
# Build and load image
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload_image_load

# Start with docker-compose
cd com/tm/docker
docker-compose up -d minio hbase hbase_bulkload

# Attach to container for interactive menu
docker attach hbase_bulkload

# Or run interactively
docker run -it --rm \
  --network tm_default \
  -e MINIO_ENDPOINT=http://minio:9000 \
  -e HBASE_ZOOKEEPER_QUORUM=hbase \
  com.tm.kotlin.hbase_bulkload:latest
```

## Troubleshooting

### Service không start
```bash
# Check logs
docker-compose logs hbase_bulkload

# Restart service
docker-compose restart hbase_bulkload
```

### MinIO connection failed
```bash
# Check MinIO
docker-compose ps minio
docker-compose logs minio

# Restart
docker-compose restart minio
```

### HBase connection failed
```bash
# Check HBase
docker-compose ps hbase
docker-compose logs hbase

# Access shell
docker exec -it com.tm.hbase hbase shell
```

## Build Configuration

BUILD file sử dụng Bazel OCI (không cần Dockerfile):

```python
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "hbase_bulkload_lib",
    srcs = glob(["*.kt"]),
    deps = [...],
)

java_binary(
    name = "hbase_bulkload",
    main_class = "com.tm.kotlin.service.hbase_bulkload.MainKt",
    runtime_deps = [":hbase_bulkload_lib"],
)

tar(
    name = "hbase_bulkload_tar",
    srcs = ["hbase_bulkload_deploy.jar"],
)

oci_image(
    name = "hbase_bulkload_image",
    base = "@distroless_java",
    entrypoint = ["java", "-jar", "/com/tm/kotlin/service/hbase_bulkload/hbase_bulkload_deploy.jar"],
    tars = [":hbase_bulkload_tar"],
)

oci_load(
    name = "hbase_bulkload_image_load",
    image = ":hbase_bulkload_image",
    repo_tags = ["com.tm.kotlin.hbase_bulkload:latest"],
)
```

## Key Learnings

### 1. HBase DeleteColumn
- Marks all versions của column as deleted
- Perfect cho removing old segment memberships
- Efficient hơn individual Delete operations

### 2. Batch Operations
- Batch puts/deletes: 1000 rows per batch
- Reduces network round-trips
- Better performance cho large datasets

### 3. RoaringBitmap
- Compressed bitmap representation
- Efficient set operations (union, intersection, difference)
- Fast serialization/deserialization
- Ideal cho user segments

### 4. Inverted Index Pattern
- User ID → Segments mapping
- Fast segment membership queries
- Critical cho recommendation systems, audience targeting

### 5. Bazel OCI
- Single command build + load
- No Dockerfile needed
- Reproducible builds
- Distroless base image (smaller, more secure)

## Cleanup

```bash
# Stop services
docker-compose stop hbase_bulkload minio hbase

# Remove containers
docker-compose rm -f hbase_bulkload minio hbase

# Remove volumes (⚠️ deletes all data)
docker volume rm tm_minio-data tm_hbase-data
```

## Performance Tuning

### Adjust Memory
Edit docker-compose.yml:
```yaml
hbase_bulkload:
  deploy:
    resources:
      limits:
        cpus: "4"
        memory: 4G
```

### Increase Batch Size
Modify verticles:
```kotlin
if (puts.size >= 5000) {  // Increase from 1000
    table.put(puts)
    puts.clear()
}
```

## Technologies

- **Kotlin**: Service implementation
- **Vert.x**: Async verticles
- **Dagger**: Dependency injection
- **RoaringBitmap**: Compressed bitmaps
- **MinIO**: S3-compatible object storage
- **HBase**: Wide-column NoSQL database
- **Bazel**: Build system với OCI image support
- **Docker**: Containerization
