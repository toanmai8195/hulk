package com.tm.kotlin.hbase_load_test.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.inject.Inject
import kotlin.random.Random

/**
 * Verticle 2: Load test at 5000 RPS
 * - Filter 1 user from 1000 users
 * - 300 random segments from segment_for_load_test_0001 -> segment_for_load_test_9999
 */
class RandomRangeLoadTestVerticle @Inject constructor(
    hbaseClient: IHBaseClient,
    registry: PrometheusMeterRegistry
) : BaseLoadTestVerticle(hbaseClient, registry, "random_range") {

    companion object {
        const val SEGMENT_MIN = 1
        const val SEGMENT_MAX = 9999
    }

    override fun selectSegments(): List<String> {
        return (1..NUM_SEGMENTS_PER_REQUEST)
            .map { Random.nextInt(SEGMENT_MIN, SEGMENT_MAX + 1) }
            .distinct()
            .take(NUM_SEGMENTS_PER_REQUEST)
            .map { formatSegmentId(it) }
    }
}
