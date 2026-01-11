package com.tm.kotlin.service.hbase_bulkload

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
import javax.inject.Inject

/**
 * Phase 5: Bulk Load HFile V2 into HBase
 * - Load pre-generated V2 HFiles (with puts and deletes) from HDFS into HBase
 * - Uses LoadIncrementalHFiles for efficient bulk loading
 */
class BulkLoadV2WithDeletesVerticle @Inject constructor(
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting Bulk Load V2 into HBase")

        try {
            // Find the latest V2 HFile directory
            println("\n⏱️  Phase 5 Timing:")
            val findStart = System.currentTimeMillis()
            val hfilePath = findLatestV2HFileDir()
            val findDuration = System.currentTimeMillis() - findStart
            println("  ✓ Find HFiles from HDFS:  ${findDuration}ms")
            println("📂 Loading HFiles from: $hfilePath")

            // Bulk load
            val bulkLoadStart = System.currentTimeMillis()
            bulkLoad(hfilePath)
            val bulkLoadDuration = System.currentTimeMillis() - bulkLoadStart
            println("  ✓ Bulk load to HBase:     ${bulkLoadDuration}ms")

            val totalDuration = findDuration + bulkLoadDuration
            println("\n📊 Phase 5 Summary:")
            println("  Total time: ${totalDuration}ms (${totalDuration / 1000}s)")

            println("\n✅ BulkLoad V2 finished")

        } catch (e: Exception) {
            println("❌ Error in BulkLoadV2WithDeletesVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun findLatestV2HFileDir(): Path {
        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")
        val fs = FileSystem.get(conf)

        val baseDir = Path("/hbase_bulkload")
        val files = fs.listStatus(baseDir) { path ->
            path.name.startsWith("v2_")
        }

        if (files.isEmpty()) {
            throw IllegalStateException("No V2 HFiles found in $baseDir")
        }

        // Return the most recent directory
        return files.maxByOrNull { it.modificationTime }?.path
            ?: throw IllegalStateException("No V2 HFiles found")
    }

    private fun bulkLoad(hfilePath: Path) {
        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")

        // Convert Path to string without URI scheme (e.g., /hbase_bulkload/v2_xxx)
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

    override suspend fun stop() {
        println("BulkLoadV2WithDeletesVerticle shutting down...")
    }
}
