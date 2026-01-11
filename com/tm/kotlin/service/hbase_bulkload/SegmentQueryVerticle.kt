package com.tm.kotlin.service.hbase_bulkload

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.DataInputStream
import javax.inject.Inject

/**
 * Verticle 4: Queries user segments and verifies results
 * - Users 100,001-1,100,000 should return segment_v2
 * - Users 1-100,000 should be empty (marked with DeleteColumn)
 */
class SegmentQueryVerticle @Inject constructor(
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting SegmentQueryVerticle...")

        try {
            val connection = ConnectionFactory.createConnection(hbaseConfig)
            val table = connection.getTable(TableName.valueOf(bulkLoadConfig.hbaseTable))

            // Test users from different ranges
            val testUsers = listOf(
                1,          // Should be empty (deleted from V1)
                50_000,     // Should be empty (deleted from V1)
                100_000,    // Should be empty (deleted from V1)
                100_001,    // Should have segment_v2
                500_000,    // Should have segment_v2
                1_000_000,  // Should have segment_v2
                1_100_000   // Should have segment_v2
            )

            println("\n📊 Querying user segments:")
            println("=" .repeat(80))

            var successCount = 0
            var failureCount = 0

            testUsers.forEach { userId ->
                val rowKey = Bytes.toBytes(String.format("user_%010d", userId))
                val get = Get(rowKey)
                get.addFamily(Bytes.toBytes(bulkLoadConfig.columnFamily))

                val result = table.get(get)

                val hasV1 = result.containsColumn(
                    Bytes.toBytes(bulkLoadConfig.columnFamily),
                    Bytes.toBytes("segment_v1")
                )

                val hasV2 = result.containsColumn(
                    Bytes.toBytes(bulkLoadConfig.columnFamily),
                    Bytes.toBytes("segment_v2")
                )

                val expectedHasV2 = userId in 100_001..1_100_000
                val expectedEmpty = userId in 1..100_000

                val success = when {
                    expectedHasV2 -> hasV2 && !hasV1
                    expectedEmpty -> !hasV1 && !hasV2
                    else -> true
                }

                val status = if (success) "✅" else "❌"
                val segments = mutableListOf<String>()
                if (hasV1) segments.add("V1")
                if (hasV2) segments.add("V2")
                val segmentInfo = if (segments.isEmpty()) "EMPTY" else segments.joinToString(", ")

                println("$status User $userId: $segmentInfo")

                if (success) successCount++ else failureCount++

                // Show detailed segment info for some users
                if (hasV2) {
                    val v2Data = result.getValue(
                        Bytes.toBytes(bulkLoadConfig.columnFamily),
                        Bytes.toBytes("segment_v2")
                    )
                    if (v2Data != null) {
                        val bitmap = RoaringBitmap()
                        val dataInput = DataInputStream(v2Data.inputStream())
                        bitmap.deserialize(dataInput)
                        println("   └─ Segment V2 contains ${bitmap.cardinality} users")
                        println("   └─ User $userId is in segment: ${bitmap.contains(userId)}")
                    }
                }
            }

            println("=" .repeat(80))
            println("\n📈 Test Results:")
            println("  ✅ Success: $successCount")
            println("  ❌ Failure: $failureCount")
            println("  📊 Total: ${testUsers.size}")

            if (failureCount == 0) {
                println("\n🎉 All tests passed! Inverted index segment rebuild works correctly.")
            } else {
                println("\n⚠️  Some tests failed. Please review the results above.")
            }

            // Additional verification: Count total rows
            println("\n📊 Additional Statistics:")
            countSegmentRows(table)

            connection.close()
            println("\n✅ SegmentQueryVerticle completed successfully")
        } catch (e: Exception) {
            println("❌ Error in SegmentQueryVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun countSegmentRows(table: org.apache.hadoop.hbase.client.Table) {
        val scan = Scan()
        scan.addFamily(Bytes.toBytes(bulkLoadConfig.columnFamily))
        scan.limit = 10000 // Limit for performance

        var v1Count = 0
        var v2Count = 0
        var totalRows = 0

        val scanner = table.getScanner(scan)
        scanner.forEach { result ->
            totalRows++

            if (result.containsColumn(
                    Bytes.toBytes(bulkLoadConfig.columnFamily),
                    Bytes.toBytes("segment_v1")
                )
            ) {
                v1Count++
            }

            if (result.containsColumn(
                    Bytes.toBytes(bulkLoadConfig.columnFamily),
                    Bytes.toBytes("segment_v2")
                )
            ) {
                v2Count++
            }
        }

        scanner.close()

        println("  Total rows scanned: $totalRows (limited to 10,000)")
        println("  Rows with segment_v1: $v1Count")
        println("  Rows with segment_v2: $v2Count")
    }

    override suspend fun stop() {
        println("SegmentQueryVerticle shutting down...")
    }
}
