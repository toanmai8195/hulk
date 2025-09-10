package com.tm.kotlin.service.counter.handler

import com.tm.kotlin.service.counter.CounterResult
import com.tm.kotlin.service.counter.UnreadCounts
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for Chat-specific counter operations
 * Uses stateful counter only - auto-calculates unread room count from room breakdown
 */
@Singleton
class ChatCounterHandler @Inject constructor(
    private val statefulHandler: StatefulCounterHandler<String>
) {

    /**
     * Get user's unread rooms count by counting rooms with unread messages
     * This is auto-calculated, not stored separately
     */
    suspend fun getUserUnreadRoomsCount(userId: String): Long {
        val roomMessages = statefulHandler.getItemCounts("user:$userId:room_messages")
        return roomMessages.count { it.value > 0 }.toLong()
    }

    // Room unread messages (stateful - track per room)
    suspend fun incrementRoomMessages(userId: String, roomId: String, delta: Long = 1): Long {
        return statefulHandler.increment("user:$userId:room_messages", roomId, delta)
    }

    suspend fun resetRoomMessages(userId: String, roomId: String) {
        statefulHandler.reset("user:$userId:room_messages", roomId)
    }

    suspend fun getUserRoomMessages(userId: String): Map<String, Long> {
        return statefulHandler.getItemCounts("user:$userId:room_messages")
    }

    suspend fun getTotalUnreadMessages(userId: String): Long {
        return statefulHandler.getTotal("user:$userId:room_messages")
    }

    suspend fun getRoomMessageCount(userId: String, roomId: String): Long {
        return statefulHandler.getItemCount("user:$userId:room_messages", roomId)
    }

    /**
     * Mark room as read - only need to reset room messages
     * Unread room count is auto-calculated from remaining rooms with messages
     */
    suspend fun markRoomAsRead(userId: String, roomId: String): CounterResult<String> {
        return try {
            val hadUnreadMessages = statefulHandler.getItemCount("user:$userId:room_messages", roomId) > 0
            
            // Reset room messages (set to 0, don't remove the item completely)
            statefulHandler.reset("user:$userId:room_messages", roomId)
            
            // Get updated counts (auto-calculated)
            val unreadRooms = getUserUnreadRoomsCount(userId)
            val remainingMessages = getUserRoomMessages(userId)
            
            CounterResult(
                success = true,
                newTotal = unreadRooms,
                allItems = remainingMessages
            )
        } catch (e: Exception) {
            CounterResult(
                success = false,
                newTotal = 0L,
                error = e.message
            )
        }
    }

    /**
     * Handle new message created event
     * Just increment room messages - unread room count is auto-calculated
     */
    suspend fun handleMessageCreated(userId: String, roomId: String): Long {
        return incrementRoomMessages(userId, roomId)
    }

    /**
     * Handle user added to room event
     * Add existing unread messages for this user in this room
     */
    suspend fun handleUserAddedToRoom(userId: String, roomId: String, existingMessageCount: Long) {
        if (existingMessageCount > 0) {
            // Add this room's messages to the new user's unread count
            incrementRoomMessages(userId, roomId, existingMessageCount)
        }
        // No need to manually increment unread rooms - it's auto-calculated
    }

    /**
     * Get complete unread status for a user
     */
    suspend fun getUnreadStatus(userId: String): UnreadCounts {
        val unreadRooms = getUserUnreadRoomsCount(userId)
        val unreadMessagesPerRoom = getUserRoomMessages(userId)
        
        return UnreadCounts(
            userId = userId,
            unreadRooms = unreadRooms,
            unreadMessagesPerRoom = unreadMessagesPerRoom
        )
    }

    /**
     * Reset all counters for a user (useful for testing or admin operations)
     */
    suspend fun resetUserCounters(userId: String) {
        statefulHandler.reset("user:$userId:room_messages")
        // No need to reset unread_rooms - it's auto-calculated
    }

    /**
     * Get room unread count across all users (admin function) 
     * This would require scanning all users - expensive operation
     */
    fun getTotalRoomUnreadCount(roomId: String): Long {
        // TODO: This would need a separate tracking mechanism
        // or scan all users to count who has unread messages in this room
        // For now, return 0 as placeholder
        println("⚠️ getTotalRoomUnreadCount($roomId) not implemented - would be expensive")
        return 0L
    }
}