# Counter Service - Clean Architecture

Universal counter system với clean architecture pattern.

## 🎯 **Overview**

Counter service hỗ trợ 2 loại counter:
- **Stateless**: Chỉ lưu tổng số (notifications, likes, views)
- **Stateful**: Lưu cả breakdown theo items (unread messages per room)

## 🏗️ **Clean Architecture - 3 Layers**

```
┌─────────────────────────────────────────┐
│           VERTICLE LAYER                │  ← Infrastructure (HTTP/Kafka)
│  VCounterHttpApiV2, VConsumerV2         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           HANDLER LAYER                 │  ← Business Logic
│  StatelessHandler, StatefulHandler      │
│  ChatCounterHandler                     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            DAO LAYER                    │  ← Data Access
│         HBaseCounterDao                 │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           DATABASE                      │  ← Storage
│            HBase                        │
└─────────────────────────────────────────┘
```

## 📁 **File Structure**

```
counter/
├── dao/                          # Data Access Objects
│   └── CounterDao.kt             # Pure database operations
├── handler/                      # Business Logic
│   ├── StatelessCounterHandler.kt # Simple counter logic
│   ├── StatefulCounterHandler.kt  # Complex counter logic
│   └── ChatCounterHandler.kt      # Chat-specific logic
├── verticles/                    # Infrastructure Layer
│   ├── api/
│   │   └── VCounterHttpApiV2.kt  # HTTP request handling
│   └── consumer/
│       ├── VStatelessCounterConsumerV2.kt # Kafka consumer
│       └── VStatefulCounterConsumerV2.kt  # Kafka consumer
├── ServiceModuleV2.kt           # Dependency injection
├── CounterMain.kt               # Application entry point
└── *.kt                         # Models, configs, etc.
```

## 🔧 **Layer Responsibilities**

### **1. DAO Layer** (Data Access)
```kotlin
interface ICounterDao {
    suspend fun increment(key: String, columnFamily: String, qualifier: String, delta: Long): Long
    suspend fun get(key: String, columnFamily: String, qualifier: String): Long
    // ... pure database operations
}
```

**Responsibilities:**
- ✅ Raw database operations (HBase)
- ✅ Data serialization/deserialization
- ✅ Connection management
- ❌ **NO** business logic
- ❌ **NO** validation rules

### **2. Handler Layer** (Business Logic)
```kotlin
@Singleton
class StatelessCounterHandler @Inject constructor(
    private val counterDao: ICounterDao,
    private val config: CounterConfig
) {
    suspend fun increment(key: String, delta: Long): Long
    // ... business logic
}
```

**Responsibilities:**
- ✅ Business rules and validation
- ✅ Counter constraints (max values, negative checks)
- ✅ Multi-step operations
- ✅ Domain-specific logic
- ❌ **NO** HTTP/Kafka handling
- ❌ **NO** direct database calls

### **3. Verticle Layer** (Infrastructure)
```kotlin
class VCounterHttpApiV2 @Inject constructor(
    private val statelessHandler: StatelessCounterHandler,
    // ... other handlers
) : AbstractVerticle() {
    // HTTP routing and request handling only
}
```

**Responsibilities:**
- ✅ HTTP request/response handling
- ✅ Kafka message processing
- ✅ JSON serialization/deserialization
- ✅ Error handling and logging
- ❌ **NO** business logic
- ❌ **NO** direct database access

## 💡 **Benefits of Clean Architecture**

### **1. Separation of Concerns** ✅
- **DAO**: Knows only about database
- **Handler**: Knows only about business rules  
- **Verticle**: Knows only about HTTP/Kafka

### **2. Testability** ✅
```kotlin
// Test business logic in isolation
@Test
fun testIncrementWithMaxValue() {
    val mockDao = mock<ICounterDao>()
    val handler = StatelessCounterHandler(mockDao, config)
    // Test without database or HTTP
}
```

### **3. Maintainability** ✅
- Change database → Only modify DAO
- Change business rules → Only modify Handler
- Change API format → Only modify Verticle

### **4. Scalability** ✅
- Each layer can be optimized independently
- Easy to add new handlers for new use cases
- Simple to mock dependencies

### **5. Code Reusability** ✅
```kotlin
// Same handler used by different verticles
class VHttpApi(chatHandler: ChatCounterHandler)      // HTTP API
class VKafkaConsumer(chatHandler: ChatCounterHandler) // Kafka consumer
class VGraphQLApi(chatHandler: ChatCounterHandler)   // Future GraphQL
```

## 🔄 **Data Flow Example**

### **HTTP Request Flow:**
```
1. HTTP POST /api/v2/counter/stateless/user:123:notifications/increment
2. VCounterHttpApiV2 → Parse request, extract parameters
3. StatelessCounterHandler → Apply business rules, validate
4. HBaseCounterDao → Execute database increment
5. Database → Atomic increment operation
6. Response flows back up the chain
7. VCounterHttpApiV2 → Return JSON response
```

### **Kafka Message Flow:**
```
1. Kafka message: user.notification.created
2. VStatelessCounterConsumerV2 → Parse message  
3. StatelessCounterHandler → Apply business logic
4. HBaseCounterDao → Database operation
5. Log result and continue processing
```

## 📊 **Layer Dependencies**

```
Verticle Layer
    ↓ (depends on)
Handler Layer  
    ↓ (depends on)
DAO Layer
    ↓ (depends on)
Database
```

**Dependency Rule**: Higher layers can depend on lower layers, but **never** the reverse!

## 🎯 **Design Patterns Used**

### **1. Repository Pattern** (DAO Layer)
- Abstracts database operations
- Swappable implementations (HBase, Redis, etc.)

### **2. Service Layer** (Handler Layer)  
- Encapsulates business logic
- Transaction boundaries
- Domain operations

### **3. Adapter Pattern** (Verticle Layer)
- Adapts external protocols to internal APIs
- HTTP ↔ Handler, Kafka ↔ Handler

### **4. Dependency Injection**
- Constructor injection via Dagger
- Testable, flexible dependencies

## 🚀 **Usage Examples**

### **Adding New Counter Type:**
```kotlin
// 1. Create new Handler
@Singleton  
class VoteCounterHandler @Inject constructor(
    private val statefulHandler: StatefulCounterHandler<String>
) {
    suspend fun vote(pollId: String, option: String): Long {
        return statefulHandler.increment("poll:$pollId:votes", option)
    }
}

// 2. Create new Verticle (optional)
class VVoteApiV2 @Inject constructor(
    private val voteHandler: VoteCounterHandler
) : AbstractVerticle() {
    // HTTP routes for voting
}

// 3. Add to DI container
@Provides fun provideVoteHandler(...): VoteCounterHandler
```

### **Switching Database:**
```kotlin
// Only change DAO implementation
class RedisCounterDao : ICounterDao {
    override suspend fun increment(key: String, ...): Long {
        // Redis implementation
    }
}

// Update DI binding
@Provides fun provideCounterDao(): ICounterDao = RedisCounterDao(...)
```

## ✨ **Clean Code Principles**

1. **Single Responsibility**: Each class has one reason to change
2. **Open/Closed**: Open for extension, closed for modification
3. **Dependency Inversion**: Depend on abstractions, not concretions
4. **Interface Segregation**: Small, focused interfaces
5. **DRY**: Reusable components across layers

## 🚀 **Quick Start**

### Build & Run
```bash
bazel build //com/tm/kotlin/service/counter:counter_svc
bazel run //com/tm/kotlin/service/counter:counter_image_load
```

### API Examples
```bash
# Stateless counter
POST /api/v2/counter/stateless/user:123:notifications/increment
GET  /api/v2/counter/stateless/user:123:notifications

# Stateful counter  
POST /api/v2/counter/stateful/user:123:messages/increment {"item": "room456"}
GET  /api/v2/counter/stateful/user:123:messages

# Chat specific
GET  /api/v2/chat/unread/user123
POST /api/v2/chat/mark-read/user123/room456
```

### Kafka Events
```bash
# Stateless events
user.notification.created → increment notifications
user.unread_rooms.increment → increment unread rooms

# Stateful events  
message.created → increment room messages
room.read → mark room as read
```

Perfect architecture for scalable, maintainable counter service! 🎯