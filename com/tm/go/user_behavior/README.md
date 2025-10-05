# User Behavior Tracking System

Hệ thống theo dõi và phân tích hành vi người dùng với khả năng xử lý 1000+ RPS.

## Kiến trúc

```
┌─────────────┐
│   Client    │
│  (Events)   │
└──────┬──────┘
       │
       v
┌─────────────────────────────────────────┐
│         Event Collector                 │
│  - Nhận events qua HTTP/gRPC            │
│  - Tạo và quản lý sessions              │
└────┬────────────────────┬───────────────┘
     │                    │
     v                    v
┌────────────┐      ┌──────────────┐
│  HBase     │      │  BigQuery    │
│  Writer    │      │  Writer      │
│            │      │              │
│ Real-time  │      │ Batch (500)  │
│ Row Key:   │      │ Flush: 10s   │
│ session_   │      │              │
│ timestamp_ │      │ - Events     │
│ eventId    │      │ - Summaries  │
└────────────┘      └──────────────┘
     │
     v
┌────────────────────────────────────────┐
│      Behavior Analyzer                 │
│  - Most used actions                   │
│  - Action patterns                     │
│  - Anomaly detection                   │
│  - Repeated pattern detection          │
└────────────────────────────────────────┘
     │
     v
┌────────────────────────────────────────┐
│      Aggregation Job                   │
│  - Session summaries                   │
│  - Periodic aggregation (5 min)        │
│  - Auto-process expired sessions       │
└────────────────────────────────────────┘
```

## Components

### 1. Session Manager
- Quản lý lifecycle của sessions
- Auto-detect session timeout (30 phút)
- Cleanup sessions cũ (24 giờ)
- Goroutine pool để xử lý events

### 2. HBase Writer
- Lưu raw events real-time
- Row key design: `sessionId_timestamp_eventId`
- Hỗ trợ range scan theo session
- 20 workers mặc định (có thể điều chỉnh)

### 3. BigQuery Writer
- Batch writing (500 events/batch)
- Auto-flush mỗi 10 giây
- 2 tables:
  - `events`: Raw events
  - `session_summaries`: Aggregated data

### 4. Behavior Analyzer
- **Most Used Actions**: Thống kê actions phổ biến nhất
- **Action Patterns**: Tìm chuỗi hành động lặp lại
- **Anomaly Detection**:
  - Repeated actions (cùng action 5 lần liên tiếp)
  - Rapid fire events (< 100ms)
  - Stuck patterns (typing->send->back lặp lại 3+ lần)
  - Missing session close

### 5. Aggregation Job
- Chạy mỗi 5 phút
- Tự động xử lý expired sessions
- Tạo session summaries cho BigQuery

## Xử lý trường hợp đặc biệt

### 1. Session timeout khi user thoát app không gửi close event
- Session Manager tự động detect inactive sessions (30 phút)
- Auto-set endTime = lastActiveTime
- Trigger aggregation job để tạo summary

### 2. Chat pattern: typing -> send -> back lặp lại
- Behavior Analyzer detect stuck patterns
- Đánh dấu anomaly với severity "high"
- Lưu vào session summary

### 3. High volume (1000 RPS)
- Event channel buffer: 10,000
- HBase: 20 workers song song
- BigQuery: Batch 500 events, flush 10s
- Session Manager: 10 workers

## Storage Strategy

### HBase (Real-time queries)
- **Use case**: Query session events của 1 user cụ thể
- **Row key**: `sessionId_timestamp_eventId`
- **Advantages**:
  - Fast range scan theo session
  - Real-time write
  - Efficient single session lookup

### BigQuery (Analytics)
- **Use case**: Aggregated analytics, reports
- **Tables**:
  - `events`: Backup events (partitioned by timestamp)
  - `session_summaries`: Pre-aggregated data
- **Advantages**:
  - Fast analytics queries
  - Cost-effective storage
  - SQL-based analysis

### Có cần job tổng hợp?
**CÓ** - Aggregation Job cần thiết vì:
1. Pre-compute session summaries (giảm query cost)
2. Detect và lưu anomalies
3. Calculate action patterns
4. Tối ưu queries cho BigQuery

## API Endpoints

```bash
# Health check
GET /health

# Metrics
GET /metrics

# Tạo session
POST /session/create?user_id=user123

# Track event
POST /track?user_id=user123&session_id=sess456&event_type=typing&screen_name=chat

# Đóng session
POST /session/close?session_id=sess456

# Phân tích session
GET /session/analysis?session_id=sess456
```

## Cài đặt và chạy

### Build với Bazel
```bash
bazel build //com/tm/go/user_behavior:user_behavior
```

### Chạy
```bash
# Set environment variables
export HBASE_HOST=localhost
export BQ_PROJECT_ID=your-project
export BQ_DATASET=user_behavior
export PORT=8080

# Run
bazel run //com/tm/go/user_behavior:user_behavior
```

### Environment Variables
- `HBASE_HOST`: HBase host (default: localhost)
- `HBASE_TABLE`: HBase table name (default: user_behavior_events)
- `BQ_PROJECT_ID`: Google Cloud project ID
- `BQ_DATASET`: BigQuery dataset name (default: user_behavior)
- `BQ_EVENT_TABLE`: Events table (default: events)
- `BQ_SUMMARY_TABLE`: Summaries table (default: session_summaries)
- `PORT`: HTTP server port (default: 8080)

## Usage Example

```go
// Tạo event collector
config := EventCollectorConfig{
    HBaseHost:           "localhost",
    BQProjectID:         "your-project",
    EventBufferSize:     10000,
    NumSessionWorkers:   10,
    NumHBaseWorkers:     20,
    AggregationInterval: 5 * time.Minute,
}

collector, _ := NewEventCollector(config)
collector.Start(config)

// Tạo session
sessionID := collector.CreateSession("user123")

// Track events
collector.TrackEvent("user123", sessionID, EventTyping, "chat_screen", nil)
collector.TrackEvent("user123", sessionID, EventSendMessage, "chat_screen", nil)
collector.TrackEvent("user123", sessionID, EventBackToHome, "home_screen", nil)

// Phân tích
analysis, _ := collector.GetSessionAnalysis(sessionID)
fmt.Printf("Anomalies detected: %d\n", len(analysis.Anomalies))

// Đóng session
collector.CloseSession(sessionID)
```

## Performance

- **Throughput**: 1000+ RPS
- **Event latency**: < 10ms (queue)
- **HBase write**: Async, batched
- **BigQuery write**: Batched (500 events hoặc 10s)
- **Session timeout detection**: 5 phút interval
- **Memory cleanup**: 10 phút interval

## Monitoring

Metrics available at `/metrics`:
- HBase writes/errors
- BigQuery events/summaries written
- Batch counts
- Error counts
