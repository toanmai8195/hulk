package com.tm.kotlin.hbase_load_test.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.inject.Inject

/**
 * Verticle 4: Load test at 5000 RPS
 * - Filter 1 user from 1000 users
 * - 300 fixed segments: segment_for_load_test_4701 -> segment_for_load_test_5000
 */
class FixedEndLoadTestVerticle @Inject constructor(
    hbaseClient: IHBaseClient,
    registry: PrometheusMeterRegistry
) : BaseLoadTestVerticle(hbaseClient, registry, "fixed_end") {

    companion object {
        const val SEGMENT_START = 4701
        const val SEGMENT_END = 5000
    }

    // Pre-computed fixed segments for performance
    private val fixedSegments: List<String> by lazy {
        (SEGMENT_START..SEGMENT_END).map { formatSegmentId(it) }
    }

    override fun selectSegments(): List<String> {
        return fixedSegments
    }
}
