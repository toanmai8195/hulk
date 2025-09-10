package com.tm.kotlin.service.counter.handler

import com.tm.kotlin.service.counter.CounterConfig
import com.tm.kotlin.service.counter.CounterResult
import com.tm.kotlin.service.counter.dao.ICounterDao
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for Stateful Counter operations
 * Contains business logic for counter operations with item tracking
 */
@Singleton  
class StatefulCounterHandler<T> @Inject constructor(
    private val counterDao: ICounterDao,
    private val config: CounterConfig,
    private val itemSerializer: (T) -> String,
    private val itemDeserializer: (String) -> T
) {
    companion object {
        private const val TOTAL_QUALIFIER = "_total"
        private const val UPDATED_QUALIFIER = "_updated"
        private const val ITEM_PREFIX = "item_"
    }

    private fun itemQualifier(item: T): String = "$ITEM_PREFIX${itemSerializer(item)}"

    suspend fun increment(key: String, item: T? = null, delta: Long = 1): Long {
        if (delta == 0L) return getTotal(key)
        
        val currentTime = System.currentTimeMillis()
        
        // Update item count if specified
        if (item != null) {
            val currentItemCount = getItemCount(key, item)
            val newItemCount = (currentItemCount + delta).coerceAtLeast(
                if (config.allowNegative) Long.MIN_VALUE else 0L
            )
            counterDao.put(key, config.columnFamily, itemQualifier(item), newItemCount)
        }
        
        // Update total count
        val currentTotal = getTotal(key)
        val newTotal = (currentTotal + delta).coerceAtLeast(
            if (config.allowNegative) Long.MIN_VALUE else 0L
        )
        
        // Apply max value constraint
        val finalTotal = if (config.maxValue != null && newTotal > config.maxValue!!) {
            config.maxValue!!
        } else newTotal
        
        counterDao.put(key, config.columnFamily, TOTAL_QUALIFIER, finalTotal)
        counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, currentTime)
        
        return finalTotal
    }

    suspend fun decrement(key: String, item: T? = null, delta: Long = 1): Long = 
        increment(key, item, -delta)

    suspend fun getTotal(key: String): Long {
        return counterDao.get(key, config.columnFamily, TOTAL_QUALIFIER)
    }

    suspend fun getItemCount(key: String, item: T): Long {
        return counterDao.get(key, config.columnFamily, itemQualifier(item))
    }

    suspend fun getItemCounts(key: String): Map<T, Long> {
        val rowData = counterDao.getRow(key, config.columnFamily)
        val itemCounts = mutableMapOf<T, Long>()
        
        rowData.forEach { (qualifier, value) ->
            if (qualifier.startsWith(ITEM_PREFIX)) {
                try {
                    val itemStr = qualifier.removePrefix(ITEM_PREFIX)
                    val item = itemDeserializer(itemStr)
                    val count = Bytes.toLong(value)
                    if (count > 0 || config.allowNegative) {
                        itemCounts[item] = count
                    }
                } catch (e: Exception) {
                    // Skip invalid items
                }
            }
        }
        
        return itemCounts
    }

    suspend fun reset(key: String, item: T? = null) {
        if (item == null) {
            // Reset entire counter
            counterDao.delete(key, config.columnFamily)
        } else {
            // Reset specific item
            val currentTotal = getTotal(key)
            val currentItemCount = getItemCount(key, item)
            val newTotal = (currentTotal - currentItemCount).coerceAtLeast(0L)
            
            counterDao.put(key, config.columnFamily, itemQualifier(item), 0L)
            counterDao.put(key, config.columnFamily, TOTAL_QUALIFIER, newTotal)
            counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, System.currentTimeMillis())
        }
    }

    suspend fun removeItem(key: String, item: T) {
        val currentTotal = getTotal(key)
        val currentItemCount = getItemCount(key, item)
        val newTotal = (currentTotal - currentItemCount).coerceAtLeast(0L)
        
        // Delete specific item column
        counterDao.delete(key, config.columnFamily, itemQualifier(item))
        
        // Update total
        counterDao.put(key, config.columnFamily, TOTAL_QUALIFIER, newTotal)
        counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, System.currentTimeMillis())
    }

    suspend fun getDetailedCounts(key: String): CounterResult<T> {
        return try {
            val total = getTotal(key)
            val itemCounts = getItemCounts(key)
            
            CounterResult(
                success = true,
                newTotal = total,
                allItems = itemCounts
            )
        } catch (e: Exception) {
            CounterResult(
                success = false,
                newTotal = 0L,
                error = e.message
            )
        }
    }

    suspend fun batchIncrementItems(key: String, itemDeltas: Map<T, Long>): CounterResult<T> {
        if (itemDeltas.isEmpty()) return getDetailedCounts(key)
        
        return try {
            val currentTime = System.currentTimeMillis()
            var totalDelta = 0L
            
            // Update each item
            itemDeltas.forEach { (item, delta) ->
                if (delta != 0L) {
                    val currentItemCount = getItemCount(key, item)
                    val newItemCount = (currentItemCount + delta).coerceAtLeast(
                        if (config.allowNegative) Long.MIN_VALUE else 0L
                    )
                    counterDao.put(key, config.columnFamily, itemQualifier(item), newItemCount)
                    totalDelta += delta
                }
            }
            
            // Update total
            val currentTotal = getTotal(key)
            val newTotal = (currentTotal + totalDelta).coerceAtLeast(
                if (config.allowNegative) Long.MIN_VALUE else 0L
            )
            val finalTotal = if (config.maxValue != null && newTotal > config.maxValue!!) {
                config.maxValue!!
            } else newTotal
            
            counterDao.put(key, config.columnFamily, TOTAL_QUALIFIER, finalTotal)
            counterDao.put(key, config.columnFamily, UPDATED_QUALIFIER, currentTime)
            
            getDetailedCounts(key)
        } catch (e: Exception) {
            CounterResult(
                success = false,
                newTotal = getTotal(key),
                error = e.message
            )
        }
    }

    suspend fun getTopItems(key: String, limit: Int = 10): List<Pair<T, Long>> {
        val itemCounts = getItemCounts(key)
        return itemCounts.toList()
            .sortedByDescending { it.second }
            .take(limit)
    }
}