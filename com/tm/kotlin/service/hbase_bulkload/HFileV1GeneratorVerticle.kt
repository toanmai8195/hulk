package com.tm.kotlin.service.hbase_bulkload

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.CellUtil
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

class HFileV1GeneratorVerticle @Inject constructor(
    private val minioClient: MinioClient,
    private val bulkLoadConfig: BulkLoadConfig,
    private val hbaseConfig: Configuration
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting HFile V1 Generation")

        val bitmap = readSegmentFromMinio("segment_v1.bin")
        println("✅ Loaded bitmap: ${bitmap.cardinality} users")

        val hfilePath = generateHFiles(bitmap)
        println("✅ HFiles V1 generated at $hfilePath")
    }

    private fun readSegmentFromMinio(fileName: String): RoaringBitmap {
        val inputStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bulkLoadConfig.minioBucket)
                .`object`("segments/$fileName")
                .build()
        )

        val bitmap = RoaringBitmap()
        bitmap.deserialize(DataInputStream(inputStream))
        inputStream.close()
        return bitmap
    }

    private fun generateHFiles(bitmap: RoaringBitmap): Path {
        val tableName = TableName.valueOf(bulkLoadConfig.hbaseTable)
        val family = Bytes.toBytes(bulkLoadConfig.columnFamily)
        val qualifier = Bytes.toBytes("segment_v1")

        val conf = HBaseConfiguration.create(hbaseConfig)
        conf.set("fs.defaultFS", bulkLoadConfig.hdfsUri)
        conf.set("hadoop.native.lib", "false")
        conf.set("dfs.permissions.enabled", "false")
        conf.set("fs.permissions.umask-mode", "000")
        conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem")
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
        conf.setBoolean("dfs.support.append", true)

        val connection = ConnectionFactory.createConnection(conf)
        val regionLocator = connection.getRegionLocator(tableName)
        val fs = FileSystem.get(conf)

        val outputDir = Path("/hbase_bulkload/v1_${System.currentTimeMillis()}")
        val familyDir = Path(outputDir, bulkLoadConfig.columnFamily)
        fs.mkdirs(familyDir)

        println("📂 Writing HFiles under: $familyDir")

        val regionStarts = regionLocator.startKeys
        println("📍 Number of regions = ${regionStarts.size}")

        val writers = mutableListOf<HFile.Writer>()

        val hfileContext = org.apache.hadoop.hbase.io.hfile.HFileContextBuilder()
            .withBlockSize(64 * 1024)
            .build()

        // Create one HFile writer per region
        for (i in regionStarts.indices) {
            val file = Path(familyDir, "region_$i.hfile")

            val writer = HFile.getWriterFactory(conf, CacheConfig(conf))
                .withPath(fs, file)
                .withFileContext(hfileContext)
                .create()

            writers.add(writer)
        }

        println("✍️ Writing rows...")
        bitmap.forEach { userId ->
            val rowKey = Bytes.toBytes(String.format("user_%010d", userId))

            val regionIdx = findRegion(rowKey, regionStarts.toList())

            val cell = CellUtil.createCell(
                rowKey,
                family,
                qualifier,
                1,
                KeyValue.Type.Put.code.toByte(),
                Bytes.toBytes(1)
            )

            writers[regionIdx].append(cell)
        }

        writers.forEach { it.close() }
        regionLocator.close()
        connection.close()

        return outputDir
    }

    private fun findRegion(rowKey: ByteArray, regionStarts: List<ByteArray>): Int {
        for (i in regionStarts.indices.reversed()) {
            if (Bytes.compareTo(rowKey, regionStarts[i]) >= 0) {
                return i
            }
        }
        return 0
    }
}