# Kafka + Flink Demo

Demo ứng dụng Kafka producer và Flink consumer đơn giản với Kotlin, Coroutines, Bazel.

## Quick Start với Docker

```bash
# 1. Start Kafka & Zookeeper containers
cd com/tm/docker
docker-compose up -d zookeeper-kafka kafka

# 2. Create topic
docker exec kafka kafka-topics --create --topic demo-topic \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# 3. Verify Kafka is running
nc -zv localhost 9092
```

## Build

```bash
# Build producer
bazel build //com/tm/kotlin/kafka_flink/producer:producer

# Build consumer
bazel build //com/tm/kotlin/kafka_flink/consumer:consumer
```

## Run

```bash
# Terminal 1 - Run Kafka Producer (gửi messages mỗi giây)
bazel run //com/tm/kotlin/kafka_flink/producer:producer

# Terminal 2 - Run Flink Consumer (nhận và xử lý messages)
bazel run //com/tm/kotlin/kafka_flink/consumer:consumer
```

## Expected Output

**Producer**:
```
🚀 Kafka Producer started - publishing to topic: demo-topic
[1] ✅ Sent to partition 0 at offset 0
[2] ✅ Sent to partition 0 at offset 1
[3] ✅ Sent to partition 0 at offset 2
...
```

**Consumer**:
```
🚀 Flink Consumer started - consuming from topic: demo-topic
📨 Received: {"id":1,"message":"Hello from Kafka producer","timestamp":1759647113705}
Processed[1]: Hello from Kafka producer (timestamp=1759647113705)
📨 Received: {"id":2,"message":"Hello from Kafka producer","timestamp":1759647114857}
Processed[2]: Hello from Kafka producer (timestamp=1759647114857)
...
```

**Verify Messages** (optional):
```bash
# Check messages in Kafka directly
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic demo-topic \
  --from-beginning \
  --max-messages 5
```

## Architecture

```
Producer (Kotlin Coroutines)
    ↓
Kafka Topic (demo-topic)
    ↓
Flink Consumer (Stream Processing)
    ↓
Console Output
```

## Word Count Demo

Ví dụ thực tế: **Đếm từ real-time** với Flink windowing.

### Run Word Count
```bash
./word_count_demo.sh
```

Hoặc manual:
```bash
# Create topic
docker exec kafka kafka-topics --create --topic word-count-topic --bootstrap-server localhost:9092

# Terminal 1 - Producer (gửi câu random mỗi 2s)
bazel run //com/tm/kotlin/kafka_flink/producer:word_count_producer

# Terminal 2 - Consumer (đếm từ trong window 10s)
bazel run //com/tm/kotlin/kafka_flink/consumer:word_count_consumer
```

### Output Example
**Producer gửi**:
```
[1] ✅ Sent: "Apache Flink is a powerful stream processing framework"
[2] ✅ Sent: "Kafka is a distributed streaming platform"
[3] ✅ Sent: "Flink processes data in real time"
```

**Consumer đếm** (mỗi 10s window):
```
📊 Word: 'flink' | Count: 3
📊 Word: 'kafka' | Count: 2
📊 Word: 'stream' | Count: 2
📊 Word: 'processing' | Count: 2
📊 Word: 'real' | Count: 1
📊 Word: 'time' | Count: 1
...
```

### How it works
1. Producer gửi sentences ngẫu nhiên về Kafka, Flink, streaming
2. Flink consumer:
   - Parse câu thành từ
   - Lowercase và clean
   - Group by word (keyBy)
   - Window 10 giây (tumbling window)
   - Sum count trong window
   - Print kết quả

## Files

- `producer/KafkaProducerApp.kt` - Kafka producer với coroutines
- `producer/WordCountProducer.kt` - Producer gửi sentences cho word count
- `consumer/FlinkConsumerApp.kt` - Flink streaming consumer
- `consumer/WordCountConsumer.kt` - Flink word count với windowing
- `producer/BUILD` - Bazel build cho producer
- `consumer/BUILD` - Bazel build cho consumer
- `word_count_demo.sh` - Script chạy word count demo
