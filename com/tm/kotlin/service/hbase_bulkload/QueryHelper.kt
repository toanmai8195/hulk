package com.tm.kotlin.service.hbase_bulkload

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes

/**
 * Helper for querying HBase segment data
 */
object QueryHelper {

    /**
     * Query and display segment information for specific users
     */
    fun queryUsers(
        connection: Connection,
        tableName: String,
        columnFamily: String,
        userIds: List<Int>,
        phaseName: String
    ) {
        val table = connection.getTable(TableName.valueOf(tableName))
        val family = Bytes.toBytes(columnFamily)
        val qualifierV1 = Bytes.toBytes("segment_v1")
        val qualifierV2 = Bytes.toBytes("segment_v2")

        println("\n" + "=".repeat(70))
        println("📊 Query Results - $phaseName")
        println("=".repeat(70))

        userIds.forEach { userId ->
            val rowKey = Bytes.toBytes(String.format("user_%010d", userId))
            val get = Get(rowKey)
            get.addFamily(family)

            val result = table.get(get)

            val hasV1 = result.containsColumn(family, qualifierV1)
            val hasV2 = result.containsColumn(family, qualifierV2)

            val segments = mutableListOf<String>()
            if (hasV1) segments.add("V1")
            if (hasV2) segments.add("V2")
            val segmentInfo = if (segments.isEmpty()) "EMPTY" else segments.joinToString(", ")

            // Determine expected state based on user ID
            val expectedState = when {
                userId <= 10_000_000 -> "V1 only"
                userId in 10_000_001..50_000_000 -> "V1 (or V2 after phase 5)"
                userId in 50_000_001..60_000_000 -> "V2 only"
                else -> "Not in any segment"
            }

            println("  User %,10d: %-15s (Expected: %s)".format(userId, segmentInfo, expectedState))

            // Show version details
            if (hasV1) {
                val v1Cell = result.getColumnLatestCell(family, qualifierV1)
                if (v1Cell != null) {
                    val timestamp = v1Cell.timestamp
                    val type = v1Cell.type
                    println("    └─ V1: timestamp=$timestamp, type=$type")
                }
            }

            if (hasV2) {
                val v2Cell = result.getColumnLatestCell(family, qualifierV2)
                if (v2Cell != null) {
                    val timestamp = v2Cell.timestamp
                    val type = v2Cell.type
                    val value = Bytes.toInt(v2Cell.valueArray, v2Cell.valueOffset, v2Cell.valueLength)
                    println("    └─ V2: timestamp=$timestamp, type=$type, value=$value")
                }
            }
        }

        println("=".repeat(70) + "\n")
        table.close()
    }

    /**
     * Validate query results against expected state
     */
    fun validateAfterPhase5(
        connection: Connection,
        tableName: String,
        columnFamily: String,
        userIds: List<Int>
    ): Pair<Int, Int> {
        val table = connection.getTable(TableName.valueOf(tableName))
        val family = Bytes.toBytes(columnFamily)
        val qualifierV1 = Bytes.toBytes("segment_v1")
        val qualifierV2 = Bytes.toBytes("segment_v2")

        var successCount = 0
        var failureCount = 0

        println("\n" + "=".repeat(70))
        println("🧪 Validation Results (After Phase 5)")
        println("=".repeat(70))

        userIds.forEach { userId ->
            val rowKey = Bytes.toBytes(String.format("user_%010d", userId))
            val get = Get(rowKey)
            get.addFamily(family)

            val result = table.get(get)

            val hasV1 = result.containsColumn(family, qualifierV1)
            val hasV2 = result.containsColumn(family, qualifierV2)

            // Expected state after phase 5:
            // - Users 1-10M: EMPTY (V1 deleted, not in V2)
            // - Users 10M-50M: V2 only (V1 deleted, V2 added)
            // - Users 50M-60M: V2 only (not in V1, V2 added)
            val expected = when {
                userId <= 10_000_000 -> "EMPTY"
                userId in 10_000_001..60_000_000 -> "V2"
                else -> "EMPTY"
            }

            val actual = when {
                hasV1 && hasV2 -> "V1+V2"
                hasV1 -> "V1"
                hasV2 -> "V2"
                else -> "EMPTY"
            }

            val isCorrect = when (expected) {
                "EMPTY" -> !hasV1 && !hasV2
                "V2" -> !hasV1 && hasV2
                else -> false
            }

            val status = if (isCorrect) "✅" else "❌"

            println("  $status User %,10d: %-10s (Expected: %-10s)".format(userId, actual, expected))

            if (isCorrect) successCount++ else failureCount++
        }

        println("=".repeat(70))
        println("  ✅ Success: $successCount")
        println("  ❌ Failure: $failureCount")
        println("=".repeat(70) + "\n")

        table.close()
        return Pair(successCount, failureCount)
    }
}
