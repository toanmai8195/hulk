package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

var msgChan = make(chan *kafka.Message, 100)

func main() {
	consumer, err := kafka.NewConsumer(
		&kafka.ConfigMap{
			"bootstrap.servers": "db-dev.mservice.io:9092",
			"group.id":          "dc-chat.evn.consumer.group",
			"auto.offset.reset": "earliest",
			"security.protocol": "SASL_SSL",
			"sasl.mechanisms":   "SCRAM-SHA-512",
			"sasl.username":     "dc-chat",
			"sasl.password":     "J0MnczOXsAQ8yYW87Lx64f6z5iHn7AWx",
		})

	if err != nil {
		panic(err)
	}

	defer consumer.Close()

	go func() {
		for msg := range msgChan {
			go handleMessage(msg)
		}
	}()

	topic := "helios.message.event-evn"

	err = consumer.SubscribeTopics([]string{topic}, nil)
	if err != nil {
		panic(err)
	}

	signchan := make(chan os.Signal, 1)
	signal.Notify(signchan, syscall.SIGINT, syscall.SIGTERM)

	fmt.Println("Bắt đầu lắng nghe tin nhắn...")

	run := true
	for run {
		select {
		case sig := <-signchan:
			fmt.Printf("Signal received %v, quit...\n", sig)
			run = false
		default:
			msg, err := consumer.ReadMessage(100 * time.Millisecond)
			if err == nil {
				msgChan <- msg
			} else {
				if kafkaErr, ok := err.(kafka.Error); ok && kafkaErr.Code() != kafka.ErrTimedOut {
					fmt.Fprintf(os.Stderr, "⚠️ Lỗi đọc Kafka: %v\n", err)
				}
				continue
			}
		}
	}
	fmt.Println("Consumer đã dừng.")
}

func handleMessage(msg *kafka.Message) {
	fmt.Printf("📥 [Xử lý] Topic: %s | Partition: %d | Offset: %d\n",
		*msg.TopicPartition.Topic, msg.TopicPartition.Partition, msg.TopicPartition.Offset)

	fmt.Printf("➡️ Message: %s\n", string(msg.Value))
}
