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
 * Phase 6: Queries user segments and verifies results after Phase 5
 *
 * Expected state after Phase 5:
 * - Users 1-10M: EMPTY (V1 deleted, not in V2)
 * - Users 10M-50M: V2 only (V1 deleted, V2 exists)
 * - Users 50M-60M: V2 only (not in V1, V2 exists)
 */
class SegmentQueryVerticle @Inject constructor(
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting SegmentQueryVerticle (Phase 6: Final Verification)")

        try {
            val connection = ConnectionFactory.createConnection(hbaseConfig)

            // Test users from different ranges (50M scale)
            val testUsers = listOf(
                // V1 only (gap) - should be EMPTY after phase 5
                1,
                100_000,
                5_000_000,
                10_000_000,

                // Overlap region - should have V2 only after phase 5
                10_000_001,
                20_000_000,
                30_000_000,
                40_000_000,
                50_000_000,

                // V2 only (gap) - should have V2
                50_000_001,
                55_000_000,
                60_000_000
            )

            // Validate results using QueryHelper
            val (successCount, failureCount) = QueryHelper.validateAfterPhase5(
                connection = connection,
                tableName = bulkLoadConfig.hbaseTable,
                columnFamily = bulkLoadConfig.columnFamily,
                userIds = testUsers
            )

            // Show overall results
            if (failureCount == 0) {
                println("🎉 All tests passed! Inverted index segment rebuild works correctly.")
                println("   - V1 users deleted: ✅")
                println("   - V2 users added: ✅")
                println("   - No version conflicts: ✅")
            } else {
                println("⚠️  Some tests failed ($failureCount failures)")
                println("   Please review the results above.")
            }

            // Additional verification: Sample scan
            println("\n📊 Additional Statistics:")
            val table = connection.getTable(TableName.valueOf(bulkLoadConfig.hbaseTable))
            countSegmentRows(table)
            table.close()

            connection.close()
            println("\n✅ SegmentQueryVerticle completed successfully")

        } catch (e: Exception) {
            println("❌ Error in SegmentQueryVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun countSegmentRows(table: org.apache.hadoop.hbase.client.Table) {
        println("\n  Scanning sample rows from HBase...")

        val scan = Scan()
        scan.addFamily(Bytes.toBytes(bulkLoadConfig.columnFamily))
        scan.limit = 100_000 // Sample 100k rows for stats

        var v1Count = 0
        var v2Count = 0
        var emptyCount = 0
        var totalRows = 0

        val scanner = table.getScanner(scan)
        val startTime = System.currentTimeMillis()

        scanner.forEach { result ->
            totalRows++

            val hasV1 = result.containsColumn(
                Bytes.toBytes(bulkLoadConfig.columnFamily),
                Bytes.toBytes("segment_v1")
            )

            val hasV2 = result.containsColumn(
                Bytes.toBytes(bulkLoadConfig.columnFamily),
                Bytes.toBytes("segment_v2")
            )

            if (hasV1) v1Count++
            if (hasV2) v2Count++
            if (!hasV1 && !hasV2) emptyCount++
        }

        scanner.close()

        val duration = System.currentTimeMillis() - startTime

        println("  Sample scan results (${duration}ms):")
        println("    Total rows scanned:  ${"%,d".format(totalRows)}")
        println("    Rows with V1:        ${"%,d".format(v1Count)}")
        println("    Rows with V2:        ${"%,d".format(v2Count)}")
        println("    Empty rows:          ${"%,d".format(emptyCount)}")

        if (totalRows > 0) {
            val v2Percentage = (v2Count.toDouble() / totalRows) * 100
            println("    V2 coverage:         %.1f%%".format(v2Percentage))
        }
    }

    override suspend fun stop() {
        println("SegmentQueryVerticle shutting down...")
    }
}
