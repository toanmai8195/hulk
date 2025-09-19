package main

import (
	"log"
	"sync"
	"time"
)

func main() {
	log.Println("[DirectExchange Producer] Starting... (Demo Mode)")

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

			// Simulate delay based on message type priority
			var delay time.Duration
			switch key {
			case "info":
				delay = 1 * time.Second
			case "warning":
				delay = 2 * time.Second
			case "error":
				delay = 3 * time.Second
			}
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