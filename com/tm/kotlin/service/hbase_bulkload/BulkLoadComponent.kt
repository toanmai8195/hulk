package com.tm.kotlin.service.hbase_bulkload

import dagger.Component
import io.minio.MinioClient
import org.apache.hadoop.conf.Configuration
import javax.inject.Singleton

/**
 * Dagger component for dependency injection
 */
@Singleton
@Component(modules = [BulkLoadModule::class])
interface BulkLoadComponent {
    // Phase 1: Generate Segments
    fun segmentGeneratorVerticle(): SegmentGeneratorVerticle

    // Phase 2: Create HFile V1
    fun hfileV1GeneratorVerticle(): HFileV1GeneratorVerticle

    // Phase 3: Bulk Load HFile V1
    fun bulkLoadV1Verticle(): BulkLoadV1Verticle

    // Phase 4: Create HFile V2
    fun hfileV2GeneratorVerticle(): HFileV2GeneratorVerticle

    // Phase 5: Bulk Load HFile V2
    fun bulkLoadV2WithDeletesVerticle(): BulkLoadV2WithDeletesVerticle

    // Phase 6: Query and Verify
    fun segmentQueryVerticle(): SegmentQueryVerticle

    // Expose dependencies for setup
    fun config(): BulkLoadConfig
    fun minioClient(): MinioClient
    fun hbaseConfig(): Configuration
}
