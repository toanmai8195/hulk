#!/bin/bash

echo "=== Word Count Demo with Kafka + Flink ==="
echo ""

# Check Kafka
if ! nc -zv localhost 9092 2>&1 | grep -q succeeded; then
    echo "❌ Kafka not running. Starting..."
    cd com/tm/docker
    docker-compose up -d zookeeper-kafka kafka
    sleep 5
    cd -
fi

echo "✅ Kafka is running"
echo ""

# Create topic
echo "Creating topic 'word-count-topic'..."
docker exec kafka kafka-topics --create --topic word-count-topic \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1 2>/dev/null || echo "Topic already exists"
echo ""

# Start producer
echo "🚀 Starting Word Count Producer (sends sentences every 2s)..."
bazel run //com/tm/kotlin/kafka_flink/producer:word_count_producer 2>/dev/null &
PRODUCER_PID=$!
echo "   Producer PID: $PRODUCER_PID"
echo ""

# Wait for some messages
sleep 3

# Start consumer
echo "🚀 Starting Word Count Consumer (10-second windows)..."
echo "   Aggregating word counts in real-time..."
echo ""
echo "Press Ctrl+C to stop"
echo "─────────────────────────────────────────"
bazel run //com/tm/kotlin/kafka_flink/consumer:word_count_consumer 2>/dev/null

# Cleanup
kill $PRODUCER_PID 2>/dev/null
echo ""
echo "Demo stopped"
