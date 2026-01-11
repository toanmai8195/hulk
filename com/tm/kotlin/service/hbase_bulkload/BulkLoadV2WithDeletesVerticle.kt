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
            val hfilePath = findLatestV2HFileDir()
            println("📂 Loading HFiles from: $hfilePath")

            bulkLoad(hfilePath)
            println("✅ BulkLoad V2 finished")

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
        println("📦 Loading HFiles into HBase from: $pathStr")

        // Use LoadIncrementalHFiles.run() - same as command line
        val loader = org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles(conf)
        val args = arrayOf(pathStr, bulkLoadConfig.hbaseTable)

        val exitCode = loader.run(args)
        if (exitCode != 0) {
            throw RuntimeException("BulkLoad failed with exit code: $exitCode")
        }
    }

    override suspend fun stop() {
        println("BulkLoadV2WithDeletesVerticle shutting down...")
    }
}
