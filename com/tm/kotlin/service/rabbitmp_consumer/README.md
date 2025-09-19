# RabbitMQ Consumer Service

This Kotlin service provides RabbitMQ consumers implemented using Vert.x verticles and coroutines. Each consumer type runs in its own verticle, corresponding to the producer patterns implemented in the Go service.

## Consumer Types

### 1. Simple Queue Consumer
- **Queue**: `simple_queue`
- **Pattern**: Basic queue consumer
- **Verticle**: `SimpleQueueConsumerVerticle`

### 2. Competing Consumers
- **Queue**: `competing_consumers_queue`
- **Pattern**: Multiple workers competing for tasks
- **Verticles**: 3 instances of `CompetingConsumerVerticle`
- **Features**: Fair dispatch, manual acknowledgment

### 3. Priority Queue Consumer
- **Queue**: `priority_queue`
- **Pattern**: Priority-based message processing
- **Verticle**: `PriorityQueueConsumerVerticle`
- **Features**: Processes high priority messages faster

### 4. Publish/Subscribe Consumers
- **Exchange**: `pub_sub_exchange` (fanout)
- **Pattern**: Multiple subscribers receive all messages
- **Verticles**: 3 instances of `PublishSubscribeConsumerVerticle`
- **Features**: Temporary exclusive queues

### 5. RPC Server
- **Queue**: `rpc_queue`
- **Pattern**: Request-response pattern
- **Verticle**: `RPCServerVerticle`
- **Features**: Processes requests and sends responses

### 6. Direct Exchange Consumers
- **Exchange**: `direct_exchange`
- **Routing Keys**: `info`, `warning`, `error`
- **Pattern**: Message routing based on routing keys
- **Verticles**: One `DirectExchangeConsumerVerticle` per routing key

## Building

```bash
# Build the library
bazel build //com/tm/kotlin/service/rabbitmp_consumer:rabbitmp_consumer

# Build the executable
bazel build //com/tm/kotlin/service/rabbitmp_consumer:consumer_main
```

## Running

```bash
# Run specific consumer type
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- <consumer_type>

# Available consumer types:
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- simple
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- competing
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- priority
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- pubsub
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- rpc
bazel run //com/tm/kotlin/service/rabbitmp_consumer:consumer_main -- direct
```

## Prerequisites

1. RabbitMQ server running on `localhost:5672`
2. Default guest/guest credentials
3. Corresponding Go producers to send messages

## Architecture

- **Vert.x Verticles**: Each consumer type runs in its own verticle
- **Kotlin Coroutines**: Asynchronous message processing
- **RabbitMQ Java Client**: AMQP connection management
- **SLF4J Logging**: Structured logging with simple implementation

## Message Processing

Each consumer processes messages asynchronously using Kotlin coroutines running on the Vert.x event loop dispatcher. Processing times vary based on message type and priority to simulate real-world scenarios.