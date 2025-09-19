package main

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

type SimpleQueueProducer struct {
	conn      *amqp.Connection
	channel   *amqp.Channel
	queueName string
}

func NewSimpleQueueProducer(rabbitmqURL string) (*SimpleQueueProducer, error) {
	conn, err := amqp.Dial(rabbitmqURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	return &SimpleQueueProducer{
		conn:      conn,
		channel:   channel,
		queueName: "simple_queue",
	}, nil
}

func (p *SimpleQueueProducer) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		return p.conn.Close()
	}
	return nil
}

func (p *SimpleQueueProducer) PublishMessages(ctx context.Context, messages []string) error {
	_, err := p.channel.QueueDeclare(
		p.queueName,
		false, // durable
		false, // delete when unused
		false, // exclusive
		false, // no-wait
		nil,   // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare queue: %w", err)
	}

	var wg sync.WaitGroup
	errChan := make(chan error, len(messages))

	for _, message := range messages {
		wg.Add(1)
		go func(msg string) {
			defer wg.Done()

			err := p.channel.PublishWithContext(
				ctx,
				"",           // exchange
				p.queueName,  // routing key
				false,        // mandatory
				false,        // immediate
				amqp.Publishing{
					ContentType: "text/plain",
					Body:        []byte(msg),
				},
			)

			if err != nil {
				errChan <- fmt.Errorf("failed to publish message '%s': %w", msg, err)
				return
			}

			log.Printf("[SimpleQueue Producer] Sent: '%s'", msg)
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

