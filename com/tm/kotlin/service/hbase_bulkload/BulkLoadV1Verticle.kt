package com.tm.kotlin.service.hbase_bulkload

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.ConnectionFactory
import javax.inject.Inject

/**
 * Phase 3: Bulk Load HFile V1 into HBase
 * - Load pre-generated HFiles from HDFS into HBase
 * - Uses LoadIncrementalHFiles for efficient bulk loading
 */
class BulkLoadV1Verticle @Inject constructor(
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting Bulk Load V1 into HBase")

        try {
            // Find the latest V1 HFile directory
            println("\n⏱️  Phase 3 Timing:")
            val findStart = System.currentTimeMillis()
            val hfilePath = findLatestV1HFileDir()
            val findDuration = System.currentTimeMillis() - findStart
            println("  ✓ Find HFiles from HDFS:  ${findDuration}ms")
            println("📂 Loading HFiles from: $hfilePath")

            // Bulk load
            val bulkLoadStart = System.currentTimeMillis()
            bulkLoad(hfilePath)
            val bulkLoadDuration = System.currentTimeMillis() - bulkLoadStart
            println("  ✓ Bulk load to HBase:     ${bulkLoadDuration}ms")

            // Verify data after bulk load
            println("\n🔍 Verifying loaded data...")
            val verifyStart = System.currentTimeMillis()
            verifyLoadedData()
            val verifyDuration = System.currentTimeMillis() - verifyStart
            println("  ✓ Verification:            ${verifyDuration}ms")

            val totalDuration = findDuration + bulkLoadDuration + verifyDuration
            println("\n📊 Phase 3 Summary:")
            println("  Total time: ${totalDuration}ms (${totalDuration / 1000}s)")

            println("\n✅ BulkLoad V1 finished")

        } catch (e: Exception) {
            println("❌ Error in BulkLoadV1Verticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun verifyLoadedData() {
        val connection = ConnectionFactory.createConnection(hbaseConfig)

        val testUsers = listOf(
            1,              // V1 only (gap)
            100_000,        // V1 only (gap)
            30_000_000,     // V1 (overlap region)
            55_000_000      // Not in V1 (will be in V2)
        )

        QueryHelper.queryUsers(
            connection = connection,
            tableName = bulkLoadConfig.hbaseTable,
            columnFamily = bulkLoadConfig.columnFamily,
            userIds = testUsers,
            phaseName = "After Phase 3 (V1 BulkLoad)"
        )

        connection.close()
    }

    private fun findLatestV1HFileDir(): Path {
        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")
        val fs = FileSystem.get(conf)

        val baseDir = Path("/hbase_bulkload")
        val files = fs.listStatus(baseDir) { path ->
            path.name.startsWith("v1_")
        }

        if (files.isEmpty()) {
            throw IllegalStateException("No V1 HFiles found in $baseDir")
        }

        // Return the most recent directory
        return files.maxByOrNull { it.modificationTime }?.path
            ?: throw IllegalStateException("No V1 HFiles found")
    }

    private fun bulkLoad(hfilePath: Path) {
        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")

        // Convert Path to string without URI scheme (e.g., /hbase_bulkload/v1_xxx)
        val pathStr = hfilePath.toUri().path
        println("\n📦 Loading HFiles into HBase")
        println("  Path: $pathStr")
        println("  Table: ${bulkLoadConfig.hbaseTable}")

        // Check HFile structure
        println("\n  ⏱️  Validating HFiles...")
        val validateStart = System.currentTimeMillis()
        val fs = FileSystem.get(conf)
        val familyDir = Path(pathStr, bulkLoadConfig.columnFamily)
        if (!fs.exists(familyDir)) {
            throw IllegalStateException("Column family directory not found: $familyDir")
        }

        val hfiles = fs.listStatus(familyDir).filter { it.path.name.endsWith(".hfile") }
        val validateDuration = System.currentTimeMillis() - validateStart
        println("  ✓ Found ${hfiles.size} HFiles (${validateDuration}ms)")

        // Calculate total size
        val totalSize = hfiles.sumOf { it.len }
        val sizeMB = totalSize / (1024.0 * 1024.0)
        println("  ✓ Total size: %.2f MB".format(sizeMB))

        // Use LoadIncrementalHFiles.run() - same as command line
        println("\n  ⏱️  Executing LoadIncrementalHFiles...")
        val loader = org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles(conf)
        val args = arrayOf(pathStr, bulkLoadConfig.hbaseTable)

        val loadStart = System.currentTimeMillis()
        val exitCode = loader.run(args)
        val loadDuration = System.currentTimeMillis() - loadStart

        println("  ✓ LoadIncrementalHFiles completed (${loadDuration}ms)")
        println("  ✓ Exit code: $exitCode")

        if (sizeMB > 0 && loadDuration > 0) {
            val throughput = sizeMB / (loadDuration / 1000.0)
            println("  ✓ Throughput: %.2f MB/s".format(throughput))
        }

        if (exitCode != 0) {
            throw RuntimeException("BulkLoad failed with exit code: $exitCode")
        }
    }
}