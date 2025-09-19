package main

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

type DirectExchangeProducer struct {
	conn         *amqp.Connection
	channel      *amqp.Channel
	exchangeName string
}

func NewDirectExchangeProducer(rabbitmqURL string) (*DirectExchangeProducer, error) {
	conn, err := amqp.Dial(rabbitmqURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	return &DirectExchangeProducer{
		conn:         conn,
		channel:      channel,
		exchangeName: "direct_exchange",
	}, nil
}

func (p *DirectExchangeProducer) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		return p.conn.Close()
	}
	return nil
}

func (p *DirectExchangeProducer) PublishMessages(ctx context.Context, routingKey string, messages []string) error {
	err := p.channel.ExchangeDeclare(
		p.exchangeName,
		"direct", // type
		true,     // durable
		false,    // auto-deleted
		false,    // internal
		false,    // no-wait
		nil,      // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare exchange: %w", err)
	}

	var wg sync.WaitGroup
	errChan := make(chan error, len(messages))

	for _, message := range messages {
		wg.Add(1)
		go func(msg string) {
			defer wg.Done()

			err := p.channel.PublishWithContext(
				ctx,
				p.exchangeName, // exchange
				routingKey,     // routing key
				false,          // mandatory
				false,          // immediate
				amqp.Publishing{
					ContentType: "text/plain",
					Body:        []byte(msg),
				},
			)

			if err != nil {
				errChan <- fmt.Errorf("failed to publish message '%s': %w", msg, err)
				return
			}

			log.Printf("[Direct Producer] Sent '%s' with routing key '%s'", msg, routingKey)
		}(message)

		time.Sleep(time.Second)
	}

	go func() {
		wg.Wait()
		close(errChan)
	}()

	for err := range errChan {
		if err != nil {
			return err
		}
	}

	return nil
}

func (p *DirectExchangeProducer) PublishByLevels(ctx context.Context) error {
	messageLevels := map[string][]string{
		"info":    {"Info message 1", "Info message 2"},
		"warning": {"Warning message"},
		"error":   {"Error message"},
	}

	var wg sync.WaitGroup
	errChan := make(chan error, 3)

	for routingKey, messages := range messageLevels {
		wg.Add(1)
		go func(key string, msgs []string) {
			defer wg.Done()

			time.Sleep(time.Duration(len(msgs)) * time.Second)

			if err := p.PublishMessages(ctx, key, msgs); err != nil {
				errChan <- err
			}
		}(routingKey, messages)
	}

	go func() {
		wg.Wait()
		close(errChan)
	}()

	for err := range errChan {
		if err != nil {
			return err
		}
	}

	return nil
}

