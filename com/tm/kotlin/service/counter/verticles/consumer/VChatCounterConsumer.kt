package com.tm.kotlin.service.counter.verticles.consumer

import com.tm.kotlin.service.counter.handler.ChatCounterHandler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*
import javax.inject.Inject

/**
 * Chat Counter Consumer Verticle
 * Handles chat-specific Kafka events using ChatCounterHandler
 */
class VChatCounterConsumer @Inject constructor(
    private val chatHandler: ChatCounterHandler,
    private val consumerProperties: Properties
) : CoroutineVerticle() {

    private lateinit var kafkaConsumer: KafkaConsumer<String, String>

    override suspend fun start() {
        try {
            setupKafkaConsumer()
            startMessageConsumption()
            println("✅ Chat Counter Consumer started successfully")
        } catch (e: Exception) {
            println("❌ Failed to start Chat Counter Consumer: ${e.message}")
        }
    }

    private fun setupKafkaConsumer() {
        kafkaConsumer = KafkaConsumer(consumerProperties.apply {
            setProperty("group.id", "chat-counter-consumer")
        })

        // Subscribe to chat-specific events
        kafkaConsumer.subscribe(
            listOf(
                "chat.message.created",
                "chat.room.read",
                "chat.user.added_to_room",
                "chat.user.removed_from_room"
            )
        )
    }

    private fun startMessageConsumption() {
        vertx.setPeriodic(1000) { _ ->
            try {
                val records: ConsumerRecords<String, String> = kafkaConsumer.poll(Duration.ofMillis(100))

                records.forEach { record ->
                    GlobalScope.launch {
                        try {
                            processMessage(record.topic(), JsonObject(record.value()))
                        } catch (e: Exception) {
                            println("❌ Error processing message from ${record.topic()}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Error consuming Kafka messages: ${e.message}")
            }
        }
    }

    private suspend fun processMessage(topic: String, event: JsonObject) {
        when (topic) {
            "chat.message.created" -> handleMessageCreated(event)
            "chat.room.read" -> handleRoomRead(event)
            "chat.user.added_to_room" -> handleUserAddedToRoom(event)
            "chat.user.removed_from_room" -> handleUserRemovedFromRoom(event)
            else -> println("⚠️ Unhandled chat topic: $topic")
        }
    }

    // ===== CHAT EVENT HANDLERS =====

    private suspend fun handleMessageCreated(event: JsonObject) {
        val senderId = event.getString("senderId")
        val roomId = event.getString("roomId") ?: return
        val roomMembers = event.getJsonArray("roomMembers") ?: return

        println("📨 New message in room $roomId from $senderId")

        // Process for each room member except sender
        roomMembers.forEach { member ->
            val memberId = member.toString()
            if (memberId != senderId) {
                try {
                    val newCount = chatHandler.handleMessageCreated(memberId, roomId)
                    val unreadRooms = chatHandler.getUserUnreadRoomsCount(memberId)
                    println("   💬 User $memberId: room $roomId messages → $newCount, unread rooms: $unreadRooms")
                } catch (e: Exception) {
                    println("   ❌ Error processing for user $memberId: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleRoomRead(event: JsonObject) {
        val userId = event.getString("userId") ?: return
        val roomId = event.getString("roomId") ?: return

        try {
            val result = chatHandler.markRoomAsRead(userId, roomId)
            println("👁️ User $userId read room $roomId → success: ${result.success}, unread rooms: ${result.newTotal}")

            if (!result.success) {
                println("   ❌ Error: ${result.error}")
            }
        } catch (e: Exception) {
            println("❌ Error marking room as read: ${e.message}")
        }
    }

    private suspend fun handleUserAddedToRoom(event: JsonObject) {
        val userId = event.getString("userId") ?: return
        val roomId = event.getString("roomId") ?: return
        val existingMessageCount = event.getLong("existingMessageCount", 0L)

        try {
            chatHandler.handleUserAddedToRoom(userId, roomId, existingMessageCount)

            if (existingMessageCount > 0) {
                val unreadRooms = chatHandler.getUserUnreadRoomsCount(userId)
                println("👥 User $userId added to room $roomId with $existingMessageCount unread messages, unread rooms: $unreadRooms")
            } else {
                println("👥 User $userId added to room $roomId (no unread messages)")
            }
        } catch (e: Exception) {
            println("❌ Error adding user to room: ${e.message}")
        }
    }

    private suspend fun handleUserRemovedFromRoom(event: JsonObject) {
        val userId = event.getString("userId") ?: return
        val roomId = event.getString("roomId") ?: return

        try {
            // When user is removed from room, reset their unread messages for that room
            chatHandler.resetRoomMessages(userId, roomId)
            val unreadRooms = chatHandler.getUserUnreadRoomsCount(userId)
            println("🚪 User $userId removed from room $roomId, unread rooms: $unreadRooms")
        } catch (e: Exception) {
            println("❌ Error removing user from room: ${e.message}")
        }
    }

    override suspend fun stop() {
        try {
            kafkaConsumer.close()
            println("✅ Chat Counter Consumer stopped")
        } catch (e: Exception) {
            println("❌ Error stopping Chat Counter Consumer: ${e.message}")
        }
    }
}