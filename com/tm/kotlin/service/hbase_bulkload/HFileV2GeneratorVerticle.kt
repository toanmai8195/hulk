package com.tm.kotlin.service.hbase_bulkload

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.KeyValue
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.io.hfile.CacheConfig
import org.apache.hadoop.hbase.io.hfile.HFile
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.DataInputStream
import javax.inject.Inject

/**
 * Phase 4: Create HFile V2 with V2 Users and Delete Columns from V1 not in V2
 * - Read segment_v1 and segment_v2 from MinIO
 * - Calculate users to add (V2) and users to delete (V1 - V2)
 * - Generate HFiles with Put operations for V2 users
 * - Generate HFiles with DeleteColumn operations for removed users
 * - Store HFiles in HDFS for later bulk load
 */
class HFileV2GeneratorVerticle @Inject constructor(
    private val minioClient: MinioClient,
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting HFile V2 Generation with Deletes")

        try {
            // Read both segments from MinIO
            val segmentV1 = readSegmentFromMinio("segment_v1.bin")
            println("✅ Read Segment V1 from MinIO (${"%,d".format(segmentV1.cardinality)} users)")
            println("   V1 range: ${segmentV1.first()} - ${segmentV1.last()}")

            val segmentV2 = readSegmentFromMinio("segment_v2.bin")
            println("✅ Read Segment V2 from MinIO (${"%,d".format(segmentV2.cardinality)} users)")
            println("   V2 range: ${segmentV2.first()} - ${segmentV2.last()}")

            // Calculate differences
            val newUsersInV2 = RoaringBitmap.andNot(segmentV2, segmentV1)
            val usersToDelete = RoaringBitmap.andNot(segmentV1, segmentV2)

            println("\n📊 Segment Analysis:")
            println("   New users in V2:        ${"%,d".format(newUsersInV2.cardinality)}")
            println("   Users to delete (gap):  ${"%,d".format(usersToDelete.cardinality)}")
            println("   All V1 to delete:       ${"%,d".format(segmentV1.cardinality)}")
            println("   All V2 to add:          ${"%,d".format(segmentV2.cardinality)}")

            // Generate HFiles for V2 puts and V1 deletes
            val hfilePath = generateHFilesV2(segmentV1, segmentV2)
            println("✅ HFiles V2 generated at $hfilePath")

        } catch (e: Exception) {
            println("❌ Error in HFileV2GeneratorVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun readSegmentFromMinio(fileName: String): RoaringBitmap {
        val inputStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bulkLoadConfig.minioBucket)
                .`object`("segments/$fileName")
                .build()
        )

        val bitmap = RoaringBitmap()
        val dataInput = DataInputStream(inputStream)
        bitmap.deserialize(dataInput)
        inputStream.close()
        return bitmap
    }

    private fun generateHFilesV2(
        segmentV1: RoaringBitmap,
        segmentV2: RoaringBitmap
    ): Path {
        val tableName = TableName.valueOf(bulkLoadConfig.hbaseTable)
        val family = Bytes.toBytes(bulkLoadConfig.columnFamily)
        val qualifierV1 = Bytes.toBytes("segment_v1")
        val qualifierV2 = Bytes.toBytes("segment_v2")

        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")
        conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem")
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
        conf.setBoolean("dfs.support.append", true)
        val connection = ConnectionFactory.createConnection(conf)
        val table = connection.getTable(tableName)
        val regionLocator = connection.getRegionLocator(tableName)
        val fs = FileSystem.get(conf)

        val outputDir = Path("/hbase_bulkload/v2_${System.currentTimeMillis()}")
        val familyDir = Path(outputDir, bulkLoadConfig.columnFamily)
        fs.mkdirs(familyDir)

        println("📂 Writing HFiles under: $familyDir")

        val regionSplits = regionLocator.startKeys
        val writers = mutableListOf<Pair<ByteArray, HFile.Writer>>()

        println("📍 HBase regions = ${regionSplits.size}")

        // Create HFileContext
        val hfileContext = org.apache.hadoop.hbase.io.hfile.HFileContextBuilder()
            .withBlockSize(64 * 1024)
            .withCompression(org.apache.hadoop.hbase.io.compress.Compression.Algorithm.NONE)
            .build()

        // Create 1 HFile per region
        for (i in regionSplits.indices) {
            val startKey = regionSplits[i]
            val file = Path(familyDir, "region_$i.hfile")

            val writer = HFile.getWriterFactory(conf, CacheConfig(conf))
                .withPath(fs, file)
                .withFileContext(hfileContext)
                .create()

            writers.add(startKey to writer)
        }

        // Collect all user IDs that need to be written
        val allUserIds = RoaringBitmap.or(segmentV2, segmentV1)
        println("\n✍️ Writing HFile V2 operations for ${"%,d".format(allUserIds.cardinality)} unique users...")

        var putCount = 0
        var deleteCount = 0
        var debugCount = 0

        // Write operations in sorted row key order
        allUserIds.forEach { userId ->
            val rowKey = Bytes.toBytes(String.format("user_%010d", userId))

            if (debugCount < 5) {
                val inV1 = segmentV1.contains(userId)
                val inV2 = segmentV2.contains(userId)
                println("   Debug: userId=$userId, inV1=$inV1, inV2=$inV2")
                debugCount++
            }

            val regionIndex = findRegion(rowKey, writers.map { it.first })

            // Write DeleteColumn for ALL segment_v1 (to ensure only 1 version at a time)
            if (segmentV1.contains(userId)) {
                val deleteKv = KeyValue(
                    rowKey,
                    family,
                    qualifierV1,
                    1,
                    KeyValue.Type.DeleteColumn
                )
                writers[regionIndex].second.append(deleteKv)
                deleteCount++
            }

            // Write Put for segment_v2 if user is in V2
            if (segmentV2.contains(userId)) {
                val putKv = KeyValue(rowKey, family, qualifierV2, Bytes.toBytes(1))
                writers[regionIndex].second.append(putKv)
                putCount++
            }
        }

        println("\n📊 Operations Summary:")
        println("   DeleteColumn (V1): ${"%,d".format(deleteCount)} operations")
        println("   Put (V2):          ${"%,d".format(putCount)} operations")
        println("   Total operations:  ${"%,d".format(deleteCount + putCount)}")

        writers.forEach { it.second.close() }

        table.close()
        regionLocator.close()
        connection.close()

        return outputDir
    }

    // Find correct region based on rowKey
    private fun findRegion(rowKey: ByteArray, regionStarts: List<ByteArray>): Int {
        for (i in regionStarts.indices.reversed()) {
            if (Bytes.compareTo(rowKey, regionStarts[i]) >= 0) {
                return i
            }
        }
        return 0
    }
}
