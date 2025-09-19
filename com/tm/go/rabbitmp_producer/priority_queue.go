package main

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

type PriorityMessage struct {
	Content  string
	Priority uint8
}

type PriorityQueueProducer struct {
	conn      *amqp.Connection
	channel   *amqp.Channel
	queueName string
}

func NewPriorityQueueProducer(rabbitmqURL string) (*PriorityQueueProducer, error) {
	conn, err := amqp.Dial(rabbitmqURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	return &PriorityQueueProducer{
		conn:      conn,
		channel:   channel,
		queueName: "priority_queue",
	}, nil
}

func (p *PriorityQueueProducer) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		return p.conn.Close()
	}
	return nil
}

func (p *PriorityQueueProducer) PublishMessages(ctx context.Context, messages []PriorityMessage) error {
	args := amqp.Table{
		"x-max-priority": 10,
	}

	_, err := p.channel.QueueDeclare(
		p.queueName,
		false, // durable
		false, // delete when unused
		false, // exclusive
		false, // no-wait
		args,  // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare queue: %w", err)
	}

	var wg sync.WaitGroup
	errChan := make(chan error, len(messages))

	for _, message := range messages {
		wg.Add(1)
		go func(msg PriorityMessage) {
			defer wg.Done()

			err := p.channel.PublishWithContext(
				ctx,
				"",           // exchange
				p.queueName,  // routing key
				false,        // mandatory
				false,        // immediate
				amqp.Publishing{
					ContentType: "text/plain",
					Body:        []byte(msg.Content),
					Priority:    msg.Priority,
				},
			)

			if err != nil {
				errChan <- fmt.Errorf("failed to publish message '%s': %w", msg.Content, err)
				return
			}

			log.Printf("[Priority Producer] Sent: '%s' with priority %d", msg.Content, msg.Priority)
		}(message)

		time.Sleep(500 * time.Millisecond)
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

