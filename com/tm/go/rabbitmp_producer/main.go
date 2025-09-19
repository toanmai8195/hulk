package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"
)

func main() {
	if len(os.Args) < 2 {
		log.Println("Usage: go run main.go [simple|competing|priority|pubsub|rpc|direct]")
		os.Exit(1)
	}

	producerType := os.Args[1]
	rabbitmqURL := "amqp://guest:guest@localhost:5672/"

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	switch producerType {
	case "simple":
		runSimpleQueue(ctx, rabbitmqURL)
	case "competing":
		runCompetingConsumers(ctx, rabbitmqURL)
	case "priority":
		runPriorityQueue(ctx, rabbitmqURL)
	case "pubsub":
		runPublishSubscribe(ctx, rabbitmqURL)
	case "rpc":
		runRPCPattern(ctx, rabbitmqURL)
	case "direct":
		runDirectExchange(ctx, rabbitmqURL)
	default:
		log.Printf("Unknown producer type: %s", producerType)
		log.Println("Available types: simple, competing, priority, pubsub, rpc, direct")
		os.Exit(1)
	}
}

func runSimpleQueue(ctx context.Context, rabbitmqURL string) {
	producer, err := NewSimpleQueueProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create simple queue producer: %v", err)
	}
	defer producer.Close()

	messages := []string{"Hello", "World", "Simple", "Queue"}
	log.Println("[SimpleQueue Producer] Starting...")

	if err := producer.PublishMessages(ctx, messages); err != nil {
		log.Fatalf("Failed to publish messages: %v", err)
	}

	log.Println("[SimpleQueue Producer] Finished sending messages")
}

func runCompetingConsumers(ctx context.Context, rabbitmqURL string) {
	producer, err := NewCompetingConsumersProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create competing consumers producer: %v", err)
	}
	defer producer.Close()

	var messages []string
	for i := 1; i <= 10; i++ {
		messages = append(messages, fmt.Sprintf("Task %d", i))
	}

	log.Println("[CompetingConsumers Producer] Starting...")
	time.Sleep(2 * time.Second)

	if err := producer.PublishMessages(ctx, messages); err != nil {
		log.Fatalf("Failed to publish messages: %v", err)
	}

	log.Println("[CompetingConsumers Producer] Finished sending messages")
}

func runPriorityQueue(ctx context.Context, rabbitmqURL string) {
	producer, err := NewPriorityQueueProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create priority queue producer: %v", err)
	}
	defer producer.Close()

	messages := []PriorityMessage{
		{"Low priority task", 1},
		{"High priority task", 9},
		{"Medium priority task", 5},
		{"Critical task", 10},
		{"Normal task", 3},
	}

	log.Println("[PriorityQueue Producer] Starting...")
	time.Sleep(2 * time.Second)

	if err := producer.PublishMessages(ctx, messages); err != nil {
		log.Fatalf("Failed to publish messages: %v", err)
	}

	log.Println("[PriorityQueue Producer] Finished sending messages")
}

func runPublishSubscribe(ctx context.Context, rabbitmqURL string) {
	producer, err := NewPublishSubscribeProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create pub/sub producer: %v", err)
	}
	defer producer.Close()

	messages := []string{"News 1", "News 2", "News 3"}

	log.Println("[Pub/Sub Producer] Starting...")
	time.Sleep(2 * time.Second)

	if err := producer.PublishMessages(ctx, messages); err != nil {
		log.Fatalf("Failed to publish messages: %v", err)
	}

	log.Println("[Pub/Sub Producer] Finished sending messages")
}

func runRPCPattern(ctx context.Context, rabbitmqURL string) {
	producer, err := NewRPCProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create RPC producer: %v", err)
	}
	defer producer.Close()

	requests := []string{"Calculate 2+2", "Process data", "Generate report"}

	log.Println("[RPC Producer] Starting...")
	time.Sleep(2 * time.Second)

	responses, err := producer.SendRequests(ctx, requests)
	if err != nil {
		log.Fatalf("Failed to send RPC requests: %v", err)
	}

	log.Printf("[RPC Demo] All responses received: %v", responses)
}

func runDirectExchange(ctx context.Context, rabbitmqURL string) {
	producer, err := NewDirectExchangeProducer(rabbitmqURL)
	if err != nil {
		log.Fatalf("Failed to create direct exchange producer: %v", err)
	}
	defer producer.Close()

	log.Println("[DirectExchange Producer] Starting...")
	time.Sleep(2 * time.Second)

	if err := producer.PublishByLevels(ctx); err != nil {
		log.Fatalf("Failed to publish messages: %v", err)
	}

	log.Println("[DirectExchange Producer] Finished sending messages")
}