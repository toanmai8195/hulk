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

            for (phase in phases) {
                when (phase) {
                    "1" -> runPhase1(component)
                    "2" -> runPhase2(component)
                    "3" -> runPhase3(component)
                    "4" -> runPhase4(component)
                    "5" -> runPhase5(component)
                    "6" -> runPhase6(component)
                    else -> println("⚠️  Unknown phase: $phase (valid: 1-6, all)")
                }
            }

            println("\n" + "=".repeat(70))
            println("✅ Selected phases completed: $phasesStr")
            println("=".repeat(70))
        }
    }
}

/* ============================
   Phase runners
   ============================ */

private suspend fun runPhase1(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 1: Generate Segments")
    println("=".repeat(70))
    component.segmentGeneratorVerticle().execute()
    println("✅ Phase 1 completed")
}

private suspend fun runPhase2(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 2: Create HFile V1 from Generated Segments on MinIO")
    println("=".repeat(70))
    component.hfileV1GeneratorVerticle().execute()
    println("✅ Phase 2 completed")
}

private suspend fun runPhase3(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 3: Bulk Load HFile V1 into HBase")
    println("=".repeat(70))
    component.bulkLoadV1Verticle().execute()
    println("✅ Phase 3 completed")
}

private suspend fun runPhase4(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 4: Create HFile V2 with V2 Users and Delete Columns from V1 not in V2")
    println("=".repeat(70))
    component.hfileV2GeneratorVerticle().execute()
    println("✅ Phase 4 completed")
}

private suspend fun runPhase5(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 5: Bulk Load HFile V2 into HBase")
    println("=".repeat(70))
    component.bulkLoadV2WithDeletesVerticle().execute()
    println("✅ Phase 5 completed")
}

private suspend fun runPhase6(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Phase 6: Query and Verify Results")
    println("=".repeat(70))
    component.segmentQueryVerticle().execute()
    println("✅ Phase 6 completed")
}

private suspend fun runAllPhases(component: BulkLoadComponent) {
    println("\n" + "=".repeat(70))
    println("Running All Phases (Auto Mode)")
    println("=".repeat(70))

    runPhase1(component)
    runPhase2(component)
    runPhase3(component)
    runPhase4(component)
    runPhase5(component)
    runPhase6(component)

    println("\n" + "=".repeat(70))
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

            admin.createTable(tableDescriptor)
            println("✅ Created HBase table: ${config.hbaseTable}")
        } else {
            println("✅ HBase table already exists: ${config.hbaseTable}")
        }

        admin.close()
        connection.close()
    } catch (e: Exception) {
        println("⚠️  HBase setup warning: ${e.message}")
        println("   Please ensure HBase is running at ${config.hbaseZookeeperQuorum}:${config.hbaseZookeeperPort}")
    }
}