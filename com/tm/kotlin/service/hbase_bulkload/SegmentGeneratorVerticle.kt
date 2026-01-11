package com.tm.kotlin.service.hbase_bulkload

import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.inject.Inject

/**
 * Verticle 1: Generates segments with RoaringBitmap and saves to MinIO
 * - Segment V1: users 1-1,000,000
 * - Segment V2: users 100,001-1,100,000
 */
class SegmentGeneratorVerticle @Inject constructor(
    private val minioClient: MinioClient,
    private val bulkLoadConfig: BulkLoadConfig
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting SegmentGeneratorVerticle...")

        try {
            // Generate Segment Version 1: users 1-1,000,000
            val segmentV1 = generateSegmentV1()
            saveSegmentToMinio(segmentV1, "segment_v1.bin")
            println("✅ Segment V1 generated and saved to MinIO (users: 1-1,000,000)")

            // Generate Segment Version 2: users 100,001-1,100,000
            val segmentV2 = generateSegmentV2()
            saveSegmentToMinio(segmentV2, "segment_v2.bin")
            println("✅ Segment V2 generated and saved to MinIO (users: 100,001-1,100,000)")

            println("✅ SegmentGeneratorVerticle completed successfully")
        } catch (e: Exception) {
            println("❌ Error in SegmentGeneratorVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun generateSegmentV1(): RoaringBitmap {
        val bitmap = RoaringBitmap()
        // Add users from 1 to 1,000,000
        for (userId in 1..1_000_000) {
            bitmap.add(userId)
        }
        println("  Generated Segment V1: ${bitmap.cardinality} users")
        return bitmap
    }

    private fun generateSegmentV2(): RoaringBitmap {
        val bitmap = RoaringBitmap()
        // Add users from 100,001 to 1,100,000
        for (userId in 100_001..1_100_000) {
            bitmap.add(userId)
        }
        println("  Generated Segment V2: ${bitmap.cardinality} users")
        return bitmap
    }

    private fun saveSegmentToMinio(bitmap: RoaringBitmap, fileName: String) {
        val outputStream = ByteArrayOutputStream()
        val dataOutput = DataOutputStream(outputStream)
        bitmap.serialize(dataOutput)
        dataOutput.flush()
        val bytes = outputStream.toByteArray()

        val inputStream = ByteArrayInputStream(bytes)

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bulkLoadConfig.minioBucket)
                .`object`("segments/$fileName")
                .stream(inputStream, bytes.size.toLong(), -1)
                .contentType("application/octet-stream")
                .build()
        )

        println("  Saved $fileName to MinIO (size: ${bytes.size} bytes)")
    }

    override suspend fun stop() {
        println("SegmentGeneratorVerticle shutting down...")
    }
}
