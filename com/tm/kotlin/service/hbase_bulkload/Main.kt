package com.tm.kotlin.service.hbase_bulkload

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import kotlinx.coroutines.runBlocking
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.TableDescriptorBuilder
import org.apache.hadoop.hbase.util.Bytes

/**
 * Batch job entry point for HBase Bulk Load
 *
 * This program is designed to run in:
 *  - Docker
 *  - Kubernetes Job
 *  - Airflow
 *
 * It MUST NOT read stdin or require interactive input.
 */
fun main() = runBlocking {
    println(
        """
        ╔════════════════════════════════════════════════════════════════╗
        ║         HBase Bulk Load - Segment Rebuild Service              ║
        ║                                                                ║
        ║  Inverted index segment rebuild (batch job)                    ║
        ╚════════════════════════════════════════════════════════════════╝
        """.trimIndent()
    )

    try {
        val component = DaggerBulkLoadComponent.create()

        // Bootstrap infrastructure
        setupMinIO(component)
        setupHBase(component)

        println("🚀 Starting bulkload job...")

        // =========================================================
        // 🔽 READ PHASES FROM ENV VARIABLE
        // =========================================================
        val phasesToRun = System.getenv("PHASE") ?: System.getenv("PHASES") ?: ""

        if (phasesToRun.isBlank()) {
            println("⚠️  No PHASE env variable set. Skipping all phases.")
            println("   Set PHASE=1 or PHASE=1,2,3 or PHASE=all to run phases")
        } else {
            executePhasesFromEnv(component, phasesToRun)
        }

        // =========================================================

        println("✅ Bulkload job finished successfully")



        Thread.sleep(Long.MAX_VALUE)
    } catch (e: Exception) {
        println("❌ Bulkload job failed: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

/* ============================
   Phase execution logic
   ============================ */

private suspend fun executePhasesFromEnv(component: BulkLoadComponent, phasesStr: String) {
    println("\n📋 Phases to run: $phasesStr")

    when (phasesStr.trim().lowercase()) {
        "all" -> {
            runAllPhases(component)
        }
        else -> {
            // Parse comma-separated phases: "1,2,3"
            val phases = phasesStr.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (phases.isEmpty()) {
                println("⚠️  No valid phases found in: $phasesStr")
                return
            }

            val overallStart = System.currentTimeMillis()
            val timings = mutableMapOf<String, Long>()

            for (phase in phases) {
                val duration = when (phase) {
                    "1" -> runPhase1(component)
                    "2" -> runPhase2(component)
                    "3" -> runPhase3(component)
                    "4" -> runPhase4(component)
                    "5" -> runPhase5(component)
                    "6" -> runPhase6(component)
                    else -> {
                        println("⚠️  Unknown phase: $phase (valid: 1-6, all)")
                        0L
                    }
                }
                if (duration > 0) {
                    timings["Phase $phase"] = duration
                }
            }

            val overallDuration = System.currentTimeMillis() - overallStart

            if (timings.isNotEmpty()) {
                printTimingSummary(timings, overallDuration)
            }
        }
    }
}

/* ============================
   Phase runners
   ============================ */

private suspend fun runPhase1(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 1: Generate Segments")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.segmentGeneratorVerticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 1 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runPhase2(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 2: Create HFile V1 from Generated Segments on MinIO")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.hfileV1GeneratorVerticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 2 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runPhase3(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 3: Bulk Load HFile V1 into HBase")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.bulkLoadV1Verticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 3 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runPhase4(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 4: Create HFile V2 with V2 Users and Delete Columns from V1 not in V2")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.hfileV2GeneratorVerticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 4 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runPhase5(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 5: Bulk Load HFile V2 into HBase")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.bulkLoadV2WithDeletesVerticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 5 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runPhase6(component: BulkLoadComponent): Long {
    println("\n" + "=".repeat(70))
    println("Phase 6: Query and Verify Results")
    println("=".repeat(70))
    val startTime = System.currentTimeMillis()
    component.segmentQueryVerticle().execute()
    val duration = System.currentTimeMillis() - startTime
    println("✅ Phase 6 completed (${formatDuration(duration)})")
    return duration
}

private suspend fun runAllPhases(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Running All Phases (Auto Mode)")
    println("=".repeat(70))

    val overallStart = System.currentTimeMillis()
    val timings = mutableMapOf<String, Long>()

    timings["Phase 1"] = runPhase1(component)
    timings["Phase 2"] = runPhase2(component)
    timings["Phase 3"] = runPhase3(component)
    timings["Phase 4"] = runPhase4(component)
    timings["Phase 5"] = runPhase5(component)
    timings["Phase 6"] = runPhase6(component)

    val overallDuration = System.currentTimeMillis() - overallStart

    printTimingSummary(timings, overallDuration)
}

/* ============================
   Helper functions
   ============================ */

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> {
            val remainingMinutes = minutes % 60
            val remainingSeconds = seconds % 60
            "${hours}h ${remainingMinutes}m ${remainingSeconds}s"
        }
        minutes > 0 -> {
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }
        else -> "${seconds}s (${ms}ms)"
    }
}

private fun printTimingSummary(timings: Map<String, Long>, totalDuration: Long) {
    println("\n" + "=".repeat(70))
    println("⏱️  Performance Summary")
    println("=".repeat(70))

    // Print each phase timing
    timings.entries.sortedBy { it.key }.forEach { (phase, duration) ->
        val percentage = (duration.toDouble() / totalDuration) * 100
        println("  %-15s %15s  (%5.1f%%)".format(
            phase + ":",
            formatDuration(duration),
            percentage
        ))
    }

    println("  " + "-".repeat(66))
    println("  %-15s %15s".format("Total:", formatDuration(totalDuration)))
    println("=".repeat(70))

    // Show breakdown by category
    val generateTime = (timings["Phase 1"] ?: 0L) + (timings["Phase 2"] ?: 0L) + (timings["Phase 4"] ?: 0L)
    val loadTime = (timings["Phase 3"] ?: 0L) + (timings["Phase 5"] ?: 0L)
    val verifyTime = timings["Phase 6"] ?: 0L

    if (generateTime > 0 || loadTime > 0 || verifyTime > 0) {
        println("\n📊 Time Breakdown by Category:")
        if (generateTime > 0) {
            val pct = (generateTime.toDouble() / totalDuration) * 100
            println("  Data Generation:  %s  (%5.1f%%)".format(formatDuration(generateTime), pct))
        }
        if (loadTime > 0) {
            val pct = (loadTime.toDouble() / totalDuration) * 100
            println("  Bulk Load:        %s  (%5.1f%%)".format(formatDuration(loadTime), pct))
        }
        if (verifyTime > 0) {
            val pct = (verifyTime.toDouble() / totalDuration) * 100
            println("  Verification:     %s  (%5.1f%%)".format(formatDuration(verifyTime), pct))
        }
        println()
    }

    println("✅ All phases completed successfully!")
    println("=".repeat(70))
}

/* ============================
   Infra bootstrap
   ============================ */

private fun setupMinIO(component: BulkLoadComponent) {
    println("\n🔧 Setting up MinIO...")

    val minioClient = component.minioClient()
    val config = component.config()

    try {
        val bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(config.minioBucket)
                .build()
        )

        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(config.minioBucket)
                    .build()
            )
            println("✅ Created MinIO bucket: ${config.minioBucket}")
        } else {
            println("✅ MinIO bucket already exists: ${config.minioBucket}")
        }
    } catch (e: Exception) {
        println("⚠️  MinIO setup warning: ${e.message}")
        println("   Please ensure MinIO is running at ${config.minioEndpoint}")
    }
}

private fun setupHBase(component: BulkLoadComponent) {
    println("\n🔧 Setting up HBase...")

    val config = component.config()

    try {
        val hbaseConfig = HBaseConfiguration.create()
        hbaseConfig.set("hbase.zookeeper.quorum", config.hbaseZookeeperQuorum)
        hbaseConfig.set("hbase.zookeeper.property.clientPort", config.hbaseZookeeperPort)

        val connection = ConnectionFactory.createConnection(hbaseConfig)
        val admin = connection.admin

        val tableName = TableName.valueOf(config.hbaseTable)

        if (!admin.tableExists(tableName)) {
            val columnFamily = ColumnFamilyDescriptorBuilder
                .newBuilder(Bytes.toBytes(config.columnFamily))
                .setMaxVersions(10)
                .build()

            val tableDescriptor = TableDescriptorBuilder
                .newBuilder(tableName)
                .setColumnFamily(columnFamily)
                .build()

            // Pre-split table into 10 regions for better distribution
            // Split keys for 60M users (user_0000000001 to user_0060000000)
            val splitKeys = arrayOf(
                Bytes.toBytes("user_0006000000"),  // Region 1: 0-6M
                Bytes.toBytes("user_0012000000"),  // Region 2: 6M-12M
                Bytes.toBytes("user_0018000000"),  // Region 3: 12M-18M
                Bytes.toBytes("user_0024000000"),  // Region 4: 18M-24M
                Bytes.toBytes("user_0030000000"),  // Region 5: 24M-30M
                Bytes.toBytes("user_0036000000"),  // Region 6: 30M-36M
                Bytes.toBytes("user_0042000000"),  // Region 7: 36M-42M
                Bytes.toBytes("user_0048000000"),  // Region 8: 42M-48M
                Bytes.toBytes("user_0054000000")   // Region 9: 48M-54M
                                                   // Region 10: 54M-60M+
            )

            admin.createTable(tableDescriptor, splitKeys)
            println("✅ Created HBase table: ${config.hbaseTable}")
            println("   Pre-split into ${splitKeys.size + 1} regions")
        } else {
            println("✅ HBase table already exists: ${config.hbaseTable}")

            // Show existing regions
            val regions = admin.getRegions(tableName)
            println("   Existing regions: ${regions.size}")
        }

        admin.close()
        connection.close()
    } catch (e: Exception) {
        println("⚠️  HBase setup warning: ${e.message}")
        println("   Please ensure HBase is running at ${config.hbaseZookeeperQuorum}:${config.hbaseZookeeperPort}")
    }
}