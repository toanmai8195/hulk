package main

import (
	"log"
	"os"
	"sync"
	"time"
)

// Mock DirectExchange producer for testing without RabbitMQ
func TestDirectExchangeProducer() {
	log.Println("[DirectExchange Producer] Starting... (Test Mode)")

	time.Sleep(2 * time.Second)

	messageLevels := map[string][]string{
		"info":    {"Info message 1", "Info message 2"},
		"warning": {"Warning message"},
		"error":   {"Error message"},
	}

	var wg sync.WaitGroup
	for routingKey, messages := range messageLevels {
		wg.Add(1)
		go func(key string, msgs []string) {
			defer wg.Done()

			// Simulate delay based on message type
			delay := time.Duration(len(key)) * time.Second
			time.Sleep(delay)

			for _, message := range msgs {
				log.Printf("[Direct Producer] Sent '%s' with routing key '%s'", message, key)
				time.Sleep(time.Second)
			}
		}(routingKey, messages)
	}

	wg.Wait()
	log.Println("[DirectExchange Producer] Finished sending messages")
}

func main() {
	if len(os.Args) > 1 && os.Args[1] == "test-direct" {
		TestDirectExchangeProducer()
		return
	}

	// Original main logic here...
	log.Println("Use 'test-direct' argument to run test mode")
}