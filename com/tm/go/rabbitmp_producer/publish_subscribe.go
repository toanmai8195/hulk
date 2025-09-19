package main

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

type PublishSubscribeProducer struct {
	conn         *amqp.Connection
	channel      *amqp.Channel
	exchangeName string
}

func NewPublishSubscribeProducer(rabbitmqURL string) (*PublishSubscribeProducer, error) {
	conn, err := amqp.Dial(rabbitmqURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	return &PublishSubscribeProducer{
		conn:         conn,
		channel:      channel,
		exchangeName: "pub_sub_exchange",
	}, nil
}

func (p *PublishSubscribeProducer) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		return p.conn.Close()
	}
	return nil
}

func (p *PublishSubscribeProducer) PublishMessages(ctx context.Context, messages []string) error {
	err := p.channel.ExchangeDeclare(
		p.exchangeName,
		"fanout", // type
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
				"",             // routing key
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

			log.Printf("[Pub/Sub Publisher] Sent: '%s'", msg)
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

