package com.tm.kotlin.service.counter.handler

import com.tm.kotlin.service.counter.CounterConfig
import com.tm.kotlin.service.counter.dao.ICounterDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for Stateless Counter operations
 * Contains business logic for simple counter operations (totals only)
 */
@Singleton
class StatelessCounterHandler @Inject constructor(
    private val counterDao: ICounterDao,
    private val config: CounterConfig
) {
    companion object {
        private const val COUNT_QUALIFIER = "count"
        private const val UPDATED_QUALIFIER = "updated"
    }

    suspend fun increment(key: String, delta: Long = 1): Long {
        if (delta == 0L) return getTotal(key)
        
        val newTotal = counterDao.increment(key, config.columnFamily, COUNT_QUALIFIER, delta)
        counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, System.currentTimeMillis())
        
        // Apply business rules
        return when {
            !config.allowNegative && newTotal < 0 -> {
                reset(key)
                0L
            }
            config.maxValue != null && newTotal > config.maxValue!! -> {
                counterDao.put(key, config.columnFamily, COUNT_QUALIFIER, config.maxValue!!)
                counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, System.currentTimeMillis())
                config.maxValue!!
            }
            else -> newTotal
        }
    }

    suspend fun decrement(key: String, delta: Long = 1): Long = increment(key, -delta)

    suspend fun getTotal(key: String): Long {
        return counterDao.get(key, config.columnFamily, COUNT_QUALIFIER)
    }

    suspend fun reset(key: String) {
        counterDao.put(key, config.columnFamily, COUNT_QUALIFIER, 0L)
        counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, System.currentTimeMillis())
    }

    suspend fun getLastUpdated(key: String): Long {
        return counterDao.get(key, config.columnFamily, UPDATED_QUALIFIER)
    }

    suspend fun batchGetTotals(keys: List<String>): Map<String, Long> {
        return counterDao.batchGet(keys, config.columnFamily, COUNT_QUALIFIER)
    }

    suspend fun batchIncrement(operations: Map<String, Long>): Map<String, Long> {
        val results = mutableMapOf<String, Long>()
        operations.forEach { (key, delta) ->
            try {
                results[key] = increment(key, delta)
            } catch (e: Exception) {
                results[key] = getTotal(key)
            }
        }
        return results
    }
}