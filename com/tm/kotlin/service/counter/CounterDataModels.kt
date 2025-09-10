package com.tm.kotlin.service.counter

/**
 * Core data models for Counter Service
 */

// Counter configuration
data class CounterConfig(
    val tableName: String,
    val columnFamily: String = "counters",
    val allowNegative: Boolean = false,
    val maxValue: Long? = null
)

// Counter operation result
data class CounterResult<T>(
    val success: Boolean,
    val newTotal: Long,
    val itemCount: Long? = null,
    val allItems: Map<T, Long>? = null,
    val error: String? = null
)

// Chat-specific models
data class UnreadCounts(
    val userId: String,
    val unreadRooms: Long,
    val unreadMessagesPerRoom: Map<String, Long>
)

// Kafka event models
data class MessageEvent(
    val senderId: String?,
    val roomId: String,
    val messageId: String,
    val roomMembers: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class RoomReadEvent(
    val userId: String,
    val roomId: String,
    val timestamp: Long = System.currentTimeMillis()
)