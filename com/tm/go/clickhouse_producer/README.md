# ClickHouse Producer

Producer Go Bazel để gửi events qua Kafka phân tích truy cập vào hệ thống user profile API.

## Mô tả

Service này thu thập và gửi các events về việc truy cập API user profile, bao gồm:
- Ai truy cập (accessor_id, accessor_type)
- Truy cập bao nhiêu lần (tracking qua timestamp)
- Dữ liệu lấy ra là gì (data_requested, data_returned)
- Có bị lỗi không (error_message, response_code)
- Có bị chặn quyền truy cập không (is_blocked, block_reason)

## Event Schema

### UserProfileAccessEvent Fields

| Field | Type | Mô tả |
|-------|------|-------|
| `event_id` | String | UUID duy nhất của event |
| `timestamp` | DateTime64(3) | Thời gian xảy ra event (millisecond precision) |
| `user_id` | String | ID của user được truy cập profile |
| `accessor_id` | String | ID của người/service thực hiện truy cập |
| `accessor_type` | String | Loại accessor: api_client, admin, service, bot |
| `api_endpoint` | String | API endpoint được gọi (vd: /api/v1/users/profile) |
| `http_method` | String | HTTP method: GET, POST, PUT, DELETE |
| `request_ip` | String | IP address của client thực hiện request |
| `user_agent` | String | User agent string từ request header |
| `response_code` | Int32 | HTTP response status code (200, 404, 500...) |
| `response_time_ms` | Int64 | Thời gian xử lý request (milliseconds) |
| `data_requested` | String | Loại dữ liệu được yêu cầu (comma separated) |
| `data_returned` | String | Loại dữ liệu thực tế trả về (comma separated) |
| `error_message` | String | Chi tiết lỗi nếu có (optional) |
| `is_blocked` | Bool | True nếu request bị chặn |
| `block_reason` | String | Lý do chặn: rate_limit, permission_denied, suspicious_activity |
| `session_id` | String | Session ID của user (optional) |
| `request_size_bytes` | Int64 | Kích thước request body (bytes) |
| `response_size_bytes` | Int64 | Kích thước response body (bytes) |

### Metadata Fields

| Field | Type | Mô tả |
|-------|------|-------|
| `producer_id` | String | ID của producer gửi event |
| `produced_at` | DateTime64(3) | Thời gian producer tạo event |
| `version` | String | Version của event schema |
| `environment` | String | Environment: development, staging, production |

### Phân loại accessor_type

- **api_client**: Client application thông thường
- **admin**: Admin user với quyền đặc biệt
- **service**: Internal service calls
- **bot**: Automated bots/crawlers
- **anonymous**: Unauthenticated requests

### Phân loại block_reason

- **rate_limit_exceeded**: Vượt quá giới hạn request per second/minute
- **permission_denied**: Không có quyền truy cập dữ liệu
- **suspicious_activity**: Phát hiện hoạt động đáng nghi
- **maintenance_mode**: Hệ thống đang bảo trì
- **geo_restriction**: Chặn theo vị trí địa lý

## Build và chạy

### Cách 1: Build với Bazel (Recommended)
```bash
bazel build //com/tm/go/clickhouse_producer:clickhouse_producer
```

### Cách 2: Chạy với Bazel
```bash
bazel run //com/tm/go/clickhouse_producer:clickhouse_producer
```

### Chạy với environment variables
```bash
export KAFKA_BROKERS="localhost:9092"
export KAFKA_TOPIC="user-profile-access-events"
export PRODUCER_ID="clickhouse-producer"
export ENVIRONMENT="production"

bazel run //com/tm/go/clickhouse_producer:clickhouse_producer
```

### Troubleshooting

Nếu gặp lỗi với dependencies, chạy các lệnh sau:

1. **Update go.mod dependencies:**
```bash
go mod tidy
```

2. **Update Bazel dependencies:**
```bash
bazel mod tidy
```

3. **Build lại:**
```bash
bazel build //com/tm/go/clickhouse_producer:clickhouse_producer
```

### Prerequisites

Đảm bảo các services sau đang chạy:
- Kafka cluster (localhost:9092)
- ClickHouse server (localhost:8123)

## ClickHouse Tables Setup

### 1. Bảng Queue (Kafka Engine)

```sql
-- Bảng queue để consume từ Kafka
CREATE TABLE user_profile_access_queue (
    event_id String,
    timestamp DateTime64(3),
    user_id String,
    accessor_id String,
    accessor_type String,
    api_endpoint String,
    http_method String,
    request_ip String,
    user_agent String,
    response_code Int32,
    response_time_ms Int64,
    data_requested String,
    data_returned String,
    error_message String,
    is_blocked Bool,
    block_reason String,
    session_id String,
    request_size_bytes Int64,
    response_size_bytes Int64,
    producer_id String,
    produced_at DateTime64(3),
    version String,
    environment String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'localhost:9092',
    kafka_topic_list = 'user-profile-access-events',
    kafka_group_name = 'clickhouse-consumer-group',
    kafka_format = 'JSONEachRow',
    kafka_row_delimiter = '\n',
    kafka_schema = 'event.event_id String, event.timestamp DateTime64(3), event.user_id String, event.accessor_id String, event.accessor_type String, event.api_endpoint String, event.http_method String, event.request_ip String, event.user_agent String, event.response_code Int32, event.response_time_ms Int64, event.data_requested String, event.data_returned String, event.error_message String, event.is_blocked Bool, event.block_reason String, event.session_id String, event.request_size_bytes Int64, event.response_size_bytes Int64, metadata.producer_id String, metadata.produced_at DateTime64(3), metadata.version String, metadata.environment String';
```

### 2. Bảng Middleware (Buffer/Staging)

```sql
-- Bảng middleware để xử lý dữ liệu trước khi đưa vào warehouse
CREATE TABLE user_profile_access_staging (
    event_id String,
    timestamp DateTime64(3),
    date Date MATERIALIZED toDate(timestamp),
    hour UInt8 MATERIALIZED toHour(timestamp),
    user_id String,
    accessor_id String,
    accessor_type String,
    api_endpoint String,
    http_method String,
    request_ip String,
    user_agent String,
    response_code Int32,
    response_time_ms Int64,
    data_requested String,
    data_returned String,
    error_message String,
    is_blocked Bool,
    block_reason String,
    session_id String,
    request_size_bytes Int64,
    response_size_bytes Int64,
    producer_id String,
    produced_at DateTime64(3),
    version String,
    environment String,
    processed_at DateTime64(3) DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY date
ORDER BY (timestamp, user_id, accessor_id)
TTL date + INTERVAL 7 DAY;
```

### 3. Bảng Warehouse (Final Storage)

```sql
-- Bảng warehouse để lưu trữ cuối cùng và phân tích
CREATE TABLE user_profile_access_warehouse (
    event_id String,
    timestamp DateTime64(3),
    date Date,
    hour UInt8,
    user_id String,
    accessor_id String,
    accessor_type String,
    api_endpoint String,
    http_method String,
    request_ip String,
    user_agent String,
    response_code Int32,
    response_time_ms Int64,
    data_requested String,
    data_returned String,
    error_message String,
    is_blocked Bool,
    block_reason String,
    session_id String,
    request_size_bytes Int64,
    response_size_bytes Int64,
    producer_id String,
    produced_at DateTime64(3),
    version String,
    environment String,
    processed_at DateTime64(3)
) ENGINE = MergeTree()
PARTITION BY (date, accessor_type)
ORDER BY (timestamp, user_id, accessor_id)
TTL date + INTERVAL 90 DAY;
```

### 4. Materialized Views

```sql
-- Materialized View từ queue sang staging
CREATE MATERIALIZED VIEW user_profile_access_queue_mv TO user_profile_access_staging AS
SELECT
    event_id,
    timestamp,
    user_id,
    accessor_id,
    accessor_type,
    api_endpoint,
    http_method,
    request_ip,
    user_agent,
    response_code,
    response_time_ms,
    data_requested,
    data_returned,
    error_message,
    is_blocked,
    block_reason,
    session_id,
    request_size_bytes,
    response_size_bytes,
    producer_id,
    produced_at,
    version,
    environment
FROM user_profile_access_queue;

-- Materialized View từ staging sang warehouse (với data validation)
CREATE MATERIALIZED VIEW user_profile_access_staging_mv TO user_profile_access_warehouse AS
SELECT
    event_id,
    timestamp,
    toDate(timestamp) as date,
    toHour(timestamp) as hour,
    user_id,
    accessor_id,
    accessor_type,
    api_endpoint,
    http_method,
    request_ip,
    user_agent,
    response_code,
    response_time_ms,
    data_requested,
    data_returned,
    error_message,
    is_blocked,
    block_reason,
    session_id,
    request_size_bytes,
    response_size_bytes,
    producer_id,
    produced_at,
    version,
    environment,
    processed_at
FROM user_profile_access_staging
WHERE event_id != '' AND user_id != '' AND timestamp > '2024-01-01 00:00:00';
```

### 5. Bảng thống kê tổng hợp

```sql
-- Bảng thống kê theo ngày
CREATE TABLE user_profile_access_daily_stats (
    date Date,
    user_id String,
    accessor_id String,
    accessor_type String,
    total_requests UInt64,
    successful_requests UInt64,
    blocked_requests UInt64,
    error_requests UInt64,
    avg_response_time Float32,
    total_data_transferred UInt64
) ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, user_id, accessor_id);

-- Materialized View để tính toán thống kê hàng ngày
CREATE MATERIALIZED VIEW user_profile_access_daily_stats_mv TO user_profile_access_daily_stats AS
SELECT
    date,
    user_id,
    accessor_id,
    accessor_type,
    count() as total_requests,
    countIf(response_code >= 200 AND response_code < 300 AND is_blocked = false) as successful_requests,
    countIf(is_blocked = true) as blocked_requests,
    countIf(response_code >= 400) as error_requests,
    avg(response_time_ms) as avg_response_time,
    sum(response_size_bytes) as total_data_transferred
FROM user_profile_access_warehouse
GROUP BY date, user_id, accessor_id, accessor_type;
```

## Queries phân tích thường dùng

### Top users được truy cập nhiều nhất
```sql
SELECT
    user_id,
    count() as access_count,
    countDistinct(accessor_id) as unique_accessors
FROM user_profile_access_warehouse
WHERE date >= today() - 7
GROUP BY user_id
ORDER BY access_count DESC
LIMIT 10;
```

### Phân tích lỗi theo accessor
```sql
SELECT
    accessor_id,
    accessor_type,
    count() as total_requests,
    countIf(response_code >= 400) as error_count,
    countIf(is_blocked = true) as blocked_count,
    round(countIf(response_code >= 400) * 100.0 / count(), 2) as error_rate
FROM user_profile_access_warehouse
WHERE date >= today() - 1
GROUP BY accessor_id, accessor_type
HAVING total_requests > 10
ORDER BY error_rate DESC;
```

### Response time analysis
```sql
SELECT
    accessor_type,
    quantile(0.5)(response_time_ms) as p50,
    quantile(0.95)(response_time_ms) as p95,
    quantile(0.99)(response_time_ms) as p99,
    max(response_time_ms) as max_response_time
FROM user_profile_access_warehouse
WHERE date >= today() - 1 AND response_code < 400
GROUP BY accessor_type;
```

## Dependencies

- Kafka cluster (localhost:9092)
- ClickHouse server (localhost:8123)
- Go 1.21+
- Bazel build system

## Configuration

Environment variables:
- `KAFKA_BROKERS`: Kafka broker addresses (default: localhost:9092)
- `KAFKA_TOPIC`: Kafka topic name (default: user-profile-access-events)
- `PRODUCER_ID`: Producer identifier (default: clickhouse-producer)
- `ENVIRONMENT`: Environment name (default: development)