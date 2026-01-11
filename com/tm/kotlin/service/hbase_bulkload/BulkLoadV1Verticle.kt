package com.tm.kotlin.service.hbase_bulkload

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
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

        // Find the latest V1 HFile directory
        val hfilePath = findLatestV1HFileDir()
        println("📂 Loading HFiles from: $hfilePath")

        bulkLoad(hfilePath)
        println("✅ BulkLoad V1 finished")
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
        println("📦 Loading HFiles into HBase")
        println("  Path: $pathStr")
        println("  Table: ${bulkLoadConfig.hbaseTable}")

        // Check HFile structure
        val fs = FileSystem.get(conf)
        val familyDir = Path(pathStr, bulkLoadConfig.columnFamily)
        if (!fs.exists(familyDir)) {
            throw IllegalStateException("Column family directory not found: $familyDir")
        }

        val hfiles = fs.listStatus(familyDir).filter { it.path.name.endsWith(".hfile") }
        println("  Found ${hfiles.size} HFiles in $familyDir")

        // Use LoadIncrementalHFiles.run() - same as command line
        val loader = org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles(conf)
        val args = arrayOf(pathStr, bulkLoadConfig.hbaseTable)

        println("  Running LoadIncrementalHFiles with args: ${args.joinToString(", ")}")
        val exitCode = loader.run(args)
        println("  Exit code: $exitCode")

        if (exitCode != 0) {
            throw RuntimeException("BulkLoad failed with exit code: $exitCode")
        }
    }
}