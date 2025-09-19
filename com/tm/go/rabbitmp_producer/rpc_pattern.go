package main

import (
	"context"
	"fmt"
	"log"
	"sync"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/google/uuid"
)

type RPCProducer struct {
	conn         *amqp.Connection
	channel      *amqp.Channel
	rpcQueueName string
	replyQueue   string
	responses    map[string]chan string
	mu           sync.RWMutex
}

func NewRPCProducer(rabbitmqURL string) (*RPCProducer, error) {
	conn, err := amqp.Dial(rabbitmqURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	replyQueue, err := channel.QueueDeclare(
		"",    // name (server will assign)
		false, // durable
		false, // delete when unused
		true,  // exclusive
		false, // no-wait
		nil,   // arguments
	)
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to declare reply queue: %w", err)
	}

	producer := &RPCProducer{
		conn:         conn,
		channel:      channel,
		rpcQueueName: "rpc_queue",
		replyQueue:   replyQueue.Name,
		responses:    make(map[string]chan string),
	}

	go producer.handleResponses()

	return producer, nil
}

func (p *RPCProducer) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		return p.conn.Close()
	}
	return nil
}

func (p *RPCProducer) handleResponses() {
	messages, err := p.channel.Consume(
		p.replyQueue,
		"",    // consumer
		true,  // auto-ack
		false, // exclusive
		false, // no-local
		false, // no-wait
		nil,   // args
	)
	if err != nil {
		log.Printf("Failed to register consumer: %v", err)
		return
	}

	for message := range messages {
		correlationId := message.CorrelationId
		response := string(message.Body)

		log.Printf("[RPC Client] Received response: '%s' for correlationId: %s", response, correlationId)

		p.mu.RLock()
		if responseChan, exists := p.responses[correlationId]; exists {
			select {
			case responseChan <- response:
			default:
				log.Printf("Response channel full for correlationId: %s", correlationId)
			}
		}
		p.mu.RUnlock()
	}
}

func (p *RPCProducer) SendRequest(ctx context.Context, request string) (string, error) {
	correlationId := uuid.New().String()
	responseChan := make(chan string, 1)

	p.mu.Lock()
	p.responses[correlationId] = responseChan
	p.mu.Unlock()

	defer func() {
		p.mu.Lock()
		delete(p.responses, correlationId)
		p.mu.Unlock()
		close(responseChan)
	}()

	err := p.channel.PublishWithContext(
		ctx,
		"",              // exchange
		p.rpcQueueName,  // routing key
		false,           // mandatory
		false,           // immediate
		amqp.Publishing{
			ContentType:   "text/plain",
			Body:          []byte(request),
			ReplyTo:       p.replyQueue,
			CorrelationId: correlationId,
		},
	)
	if err != nil {
		return "", fmt.Errorf("failed to publish request: %w", err)
	}

	log.Printf("[RPC Client] Sent request: '%s' with correlationId: %s", request, correlationId)

	select {
	case response := <-responseChan:
		return response, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func (p *RPCProducer) SendRequests(ctx context.Context, requests []string) ([]string, error) {
	var wg sync.WaitGroup
	responseChan := make(chan struct {
		response string
		err      error
		index    int
	}, len(requests))

	responses := make([]string, len(requests))

	for i, request := range requests {
		wg.Add(1)
		go func(idx int, req string) {
			defer wg.Done()

			response, err := p.SendRequest(ctx, req)
			responseChan <- struct {
				response string
				err      error
				index    int
			}{response, err, idx}
		}(i, request)
	}

	go func() {
		wg.Wait()
		close(responseChan)
	}()

	for result := range responseChan {
		if result.err != nil {
			return nil, result.err
		}
		responses[result.index] = result.response
	}

	return responses, nil
}

