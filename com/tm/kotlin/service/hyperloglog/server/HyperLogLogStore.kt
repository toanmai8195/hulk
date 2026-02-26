package com.tm.kotlin.service.hyperloglog.server

import net.agkn.hll.HLL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class HyperLogLogStore {
    companion object {
        private const val LOG2M = 14 // 2^14 = 16384 registers
        private const val REGWIDTH = 5 // 5 bits per register

        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
    }

    private val hlls = ConcurrentHashMap<String, HLL>()

    fun getCurrentDailyBucket(): String {
        return "daily:${LocalDate.now().format(DATE_FORMAT)}"
    }

    fun getCurrentHourlyBucket(): String {
        return "hourly:${LocalDateTime.now().format(HOUR_FORMAT)}"
    }

    @Synchronized
    fun addSegments(segments: List<String>): Pair<Long, Long> {
        val dailyBucket = getCurrentDailyBucket()
        val hourlyBucket = getCurrentHourlyBucket()

        val dailyHll = hlls.getOrPut(dailyBucket) { createHLL() }
        val hourlyHll = hlls.getOrPut(hourlyBucket) { createHLL() }

        segments.forEach { segment ->
            val hash = hashSegment(segment)
            dailyHll.addRaw(hash)
            hourlyHll.addRaw(hash)
        }

        return Pair(dailyHll.cardinality(), hourlyHll.cardinality())
    }

    fun getCardinality(timeBucket: String): Long {
        return hlls[timeBucket]?.cardinality() ?: 0
    }

    fun getAllBuckets(): Set<String> {
        return hlls.keys.toSet()
    }

    fun getHllBytes(timeBucket: String): ByteArray? {
        return hlls[timeBucket]?.toBytes()
    }

    fun getAllHllBytes(): Map<String, ByteArray> {
        return hlls.mapValues { it.value.toBytes() }
    }

    @Synchronized
    fun loadFromBytes(timeBucket: String, bytes: ByteArray) {
        val hll = HLL.fromBytes(bytes)
        hlls[timeBucket] = hll
    }

    fun getCurrentDailyCardinality(): Long {
        return getCardinality(getCurrentDailyBucket())
    }

    fun getCurrentHourlyCardinality(): Long {
        return getCardinality(getCurrentHourlyBucket())
    }

    private fun createHLL(): HLL {
        return HLL(LOG2M, REGWIDTH)
    }

    private fun hashSegment(segment: String): Long {
        var h = 0L
        for (c in segment) {
            h = 31 * h + c.code
        }
        return h
    }
}
