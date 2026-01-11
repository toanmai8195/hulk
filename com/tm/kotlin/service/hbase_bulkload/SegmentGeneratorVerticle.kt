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
 *
 * BIG MODE - 50M users per segment:
 * - Segment V1: 50M users (1 → 50,000,000)
 * - Segment V2: 50M users (10,000,001 → 60,000,000)
 *
 * Distribution:
 * - V1 only: 10M users (1 → 10,000,000)
 * - Overlap: 40M users (10,000,001 → 50,000,000)
 * - V2 only: 10M users (50,000,001 → 60,000,000)
 */
class SegmentGeneratorVerticle @Inject constructor(
    private val minioClient: MinioClient,
    private val bulkLoadConfig: BulkLoadConfig
) : CoroutineVerticle() {

    override suspend fun start() {
        execute()
    }

    suspend fun execute() {
        println("🚀 Starting SegmentGeneratorVerticle (BIG MODE: 50M users each)")

        try {
            // Generate Segment Version 1: 50M users
            println("\n📊 Generating Segment V1 (50M users)...")
            val segmentV1 = generateSegmentV1()
            saveSegmentToMinio(segmentV1, "segment_v1.bin")

            // Generate Segment Version 2: 50M users
            println("\n📊 Generating Segment V2 (50M users)...")
            val segmentV2 = generateSegmentV2()
            saveSegmentToMinio(segmentV2, "segment_v2.bin")

            // Print statistics
            val overlap = RoaringBitmap.and(segmentV1, segmentV2)
            val v1Only = RoaringBitmap.andNot(segmentV1, segmentV2)
            val v2Only = RoaringBitmap.andNot(segmentV2, segmentV1)

            println("\n" + "=".repeat(70))
            println("📈 Segment Statistics:")
            println("   V1 total:       ${"%,d".format(segmentV1.cardinality)} users")
            println("   V2 total:       ${"%,d".format(segmentV2.cardinality)} users")
            println("   Overlap:        ${"%,d".format(overlap.cardinality)} users")
            println("   V1 only (gap):  ${"%,d".format(v1Only.cardinality)} users")
            println("   V2 only (gap):  ${"%,d".format(v2Only.cardinality)} users")
            println("=".repeat(70))

            println("\n✅ SegmentGeneratorVerticle completed successfully")
        } catch (e: Exception) {
            println("❌ Error in SegmentGeneratorVerticle: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun generateSegmentV1(): RoaringBitmap {
        val bitmap = RoaringBitmap()
        val startTime = System.currentTimeMillis()

        // Add users 1 → 50,000,000 (50M users)
        bitmap.add(1L, 50_000_001L) // addRange is exclusive at end

        val duration = System.currentTimeMillis() - startTime
        println("  ✅ Generated V1: ${"%,d".format(bitmap.cardinality)} users (${duration}ms)")
        return bitmap
    }

    private fun generateSegmentV2(): RoaringBitmap {
        val bitmap = RoaringBitmap()
        val startTime = System.currentTimeMillis()

        // Add users 10,000,001 → 60,000,000 (50M users)
        // This creates:
        //   - 10M gap at start (1-10M only in V1)
        //   - 40M overlap (10M-50M in both)
        //   - 10M gap at end (50M-60M only in V2)
        bitmap.add(10_000_001L, 60_000_001L) // addRange is exclusive at end

        val duration = System.currentTimeMillis() - startTime
        println("  ✅ Generated V2: ${"%,d".format(bitmap.cardinality)} users (${duration}ms)")
        return bitmap
    }

    private fun saveSegmentToMinio(bitmap: RoaringBitmap, fileName: String) {
        // Serialize to bytes
        val serializeStart = System.currentTimeMillis()
        val outputStream = ByteArrayOutputStream()
        val dataOutput = DataOutputStream(outputStream)
        bitmap.serialize(dataOutput)
        dataOutput.flush()
        val bytes = outputStream.toByteArray()
        val serializeTime = System.currentTimeMillis() - serializeStart

        val sizeMB = bytes.size / (1024.0 * 1024.0)
        println("  📦 Serialized: %.2f MB (${serializeTime}ms)".format(sizeMB))

        // Upload to MinIO
        val uploadStart = System.currentTimeMillis()
        val inputStream = ByteArrayInputStream(bytes)

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bulkLoadConfig.minioBucket)
                .`object`("segments/$fileName")
                .stream(inputStream, bytes.size.toLong(), -1)
                .contentType("application/octet-stream")
                .build()
        )

        val uploadTime = System.currentTimeMillis() - uploadStart
        val throughputMBps = if (uploadTime > 0) sizeMB / (uploadTime / 1000.0) else 0.0
        println("  ☁️  Uploaded to MinIO: $fileName (${uploadTime}ms, %.2f MB/s)".format(throughputMBps))
    }

    override suspend fun stop() {
        println("SegmentGeneratorVerticle shutting down...")
    }
}
