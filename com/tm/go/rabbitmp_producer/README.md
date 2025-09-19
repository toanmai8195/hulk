# RabbitMQ Producers for Go

This directory contains Go implementations of RabbitMQ producers corresponding to the Kotlin consumers in `com/tm/kotlin/service/rabbitmq`.

## Producers Implemented

1. **SimpleQueue** - Basic message queue producer
2. **CompetingConsumers** - Work queue producer for load balancing
3. **PriorityQueue** - Priority-based message producer
4. **PublishSubscribe** - Fanout exchange producer for broadcasting
5. **RPCPattern** - RPC client for request/response messaging
6. **DirectExchange** - Direct exchange producer with routing keys

## Usage

### Building with Bazel

```bash
# Build the producers
bazel build //com/tm/go/rabbitmp_producer:rabbitmq_producers

# Run a specific producer type
bazel run //com/tm/go/rabbitmp_producer:rabbitmq_producers -- [producer_type]
```

### Available Producer Types

- `simple` - Simple queue producer
- `competing` - Competing consumers producer
- `priority` - Priority queue producer
- `pubsub` - Publish/subscribe producer
- `rpc` - RPC pattern producer
- `direct` - Direct exchange producer

### Examples

```bash
# Run simple queue producer
bazel run //com/tm/go/rabbitmp_producer:rabbitmq_producers -- simple

# Run priority queue producer
bazel run //com/tm/go/rabbitmp_producer:rabbitmq_producers -- priority

# Run RPC producer
bazel run //com/tm/go/rabbitmp_producer:rabbitmq_producers -- rpc
```

## Features

- **Goroutines**: Each producer uses goroutines for concurrent message publishing
- **Context support**: All producers support context for timeouts and cancellation
- **Error handling**: Proper error handling and connection management
- **Bazel integration**: Built with Bazel build system
- **RabbitMQ patterns**: Implements common RabbitMQ messaging patterns

## Prerequisites

- RabbitMQ server running on localhost:5672
- Default credentials (guest/guest)
- Go 1.24.1 or later
- Bazel build system

## Dependencies

- `github.com/rabbitmq/amqp091-go` - RabbitMQ client library
- `github.com/google/uuid` - UUID generation for RPC correlation IDs

## Architecture

Each producer follows the same pattern:

1. **Connection setup** - Establishes connection to RabbitMQ
2. **Channel creation** - Creates a channel for communication
3. **Queue/Exchange declaration** - Declares necessary queues and exchanges
4. **Message publishing** - Publishes messages using goroutines
5. **Resource cleanup** - Properly closes connections and channels

The producers are designed to work with the corresponding Kotlin consumers in the `com/tm/kotlin/service/rabbitmq` directory.