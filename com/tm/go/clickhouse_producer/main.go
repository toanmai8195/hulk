package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/signal"
	"time"

	"github.com/IBM/sarama"
	"github.com/google/uuid"
)

const (
	defaultKafkaBroker = "localhost:9092"
	defaultTopic       = "user-profile-access-events"
	defaultProducerID  = "clickhouse-producer"
	defaultEnvironment = "development"
	version            = "1.0.0"
)

type Producer struct {
	producer   sarama.SyncProducer
	topic      string
	producerID string
	env        string
}

func NewProducer(brokers []string, topic, producerID, env string) (*Producer, error) {
	config := sarama.NewConfig()
	config.Producer.RequiredAcks = sarama.WaitForAll
	config.Producer.Retry.Max = 5
	config.Producer.Return.Successes = true
	config.Producer.Compression = sarama.CompressionSnappy
	config.Producer.Partitioner = sarama.NewRandomPartitioner
	config.Producer.Idempotent = true
	config.Net.MaxOpenRequests = 1

	producer, err := sarama.NewSyncProducer(brokers, config)
	if err != nil {
		return nil, fmt.Errorf("failed to create producer: %w", err)
	}

	return &Producer{
		producer:   producer,
		topic:      topic,
		producerID: producerID,
		env:        env,
	}, nil
}

func (p *Producer) SendEvent(event UserProfileAccessEvent) error {
	kafkaMsg := KafkaMessage{
		Event: event,
		Metadata: EventMetadata{
			ProducerID:  p.producerID,
			ProducedAt:  time.Now(),
			Version:     version,
			Environment: p.env,
		},
	}

	msgBytes, err := json.Marshal(kafkaMsg)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %w", err)
	}

	msg := &sarama.ProducerMessage{
		Topic: p.topic,
		Key:   sarama.StringEncoder(event.EventID),
		Value: sarama.ByteEncoder(msgBytes),
		Headers: []sarama.RecordHeader{
			{
				Key:   []byte("event_type"),
				Value: []byte("user_profile_access"),
			},
			{
				Key:   []byte("producer_id"),
				Value: []byte(p.producerID),
			},
			{
				Key:   []byte("version"),
				Value: []byte(version),
			},
		},
	}

	partition, offset, err := p.producer.SendMessage(msg)
	if err != nil {
		return fmt.Errorf("failed to send message: %w", err)
	}

	log.Printf("Message sent to partition %d at offset %d", partition, offset)
	return nil
}

func (p *Producer) Close() error {
	return p.producer.Close()
}

func createSampleEvent(userID, accessorID string) UserProfileAccessEvent {
	return UserProfileAccessEvent{
		EventID:       uuid.New().String(),
		Timestamp:     time.Now(),
		UserID:        userID,
		AccessorID:    accessorID,
		AccessorType:  "api_client",
		APIEndpoint:   "/api/v1/users/profile",
		HTTPMethod:    "GET",
		RequestIP:     "192.168.1.100",
		UserAgent:     "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
		ResponseCode:  200,
		ResponseTime:  150,
		DataRequested: "profile_info,preferences,activity_log",
		DataReturned:  "profile_info,preferences",
		ErrorMessage:  "",
		IsBlocked:     false,
		BlockReason:   "",
		SessionID:     uuid.New().String(),
		RequestSize:   256,
		ResponseSize:  1024,
	}
}

func main() {
	brokers := []string{getEnv("KAFKA_BROKERS", defaultKafkaBroker)}
	topic := getEnv("KAFKA_TOPIC", defaultTopic)
	producerID := getEnv("PRODUCER_ID", defaultProducerID)
	environment := getEnv("ENVIRONMENT", defaultEnvironment)

	producer, err := NewProducer(brokers, topic, producerID, environment)
	if err != nil {
		log.Fatalf("Failed to create producer: %v", err)
	}
	defer producer.Close()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)

	go func() {
		<-c
		log.Println("Shutting down producer...")
		cancel()
	}()

	log.Printf("Starting Kafka producer for topic: %s", topic)
	log.Printf("Kafka brokers: %v", brokers)
	log.Printf("Producer ID: %s", producerID)
	log.Printf("Environment: %s", environment)

	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	userIDs := []string{"user_001", "user_002", "user_003", "user_004", "user_005"}
	accessorIDs := []string{"client_001", "client_002", "admin_001", "service_001"}

	for {
		select {
		case <-ctx.Done():
			log.Println("Producer stopped")
			return
		case <-ticker.C:
			userID := userIDs[time.Now().Unix()%int64(len(userIDs))]
			accessorID := accessorIDs[time.Now().Unix()%int64(len(accessorIDs))]

			event := createSampleEvent(userID, accessorID)

			if time.Now().Unix()%10 == 0 {
				event.IsBlocked = true
				event.BlockReason = "rate_limit_exceeded"
				event.ResponseCode = 429
			}

			if time.Now().Unix()%15 == 0 {
				event.ResponseCode = 500
				event.ErrorMessage = "internal_server_error"
			}

			if err := producer.SendEvent(event); err != nil {
				log.Printf("Failed to send event: %v", err)
			} else {
				log.Printf("Sent event for user %s accessed by %s", userID, accessorID)
			}
		}
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}