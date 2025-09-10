package com.tm.kotlin.service.counter.dao

import com.tm.kotlin.common.hbase.IHBaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.hadoop.hbase.client.*
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object for Counter operations
 * Pure database layer - no business logic
 */
interface ICounterDao {
    suspend fun increment(key: String, columnFamily: String, qualifier: String, delta: Long): Long
    suspend fun get(key: String, columnFamily: String, qualifier: String): Long
    suspend fun put(key: String, columnFamily: String, qualifier: String, value: Long)
    suspend fun delete(key: String, columnFamily: String, qualifier: String? = null)
    suspend fun getRow(key: String, columnFamily: String): Map<String, ByteArray>
    suspend fun batchGet(keys: List<String>, columnFamily: String, qualifier: String): Map<String, Long>
}

@Singleton
class HBaseCounterDao @Inject constructor(
    private val hbaseClient: IHBaseClient
) : ICounterDao {

    override suspend fun increment(key: String, columnFamily: String, qualifier: String, delta: Long): Long = 
        withContext(Dispatchers.IO) {
            val increment = Increment(Bytes.toBytes(key))
            increment.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier), delta)
            
            val result = hbaseClient.inc(increment)
            Bytes.toLong(result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier)))
        }

    override suspend fun get(key: String, columnFamily: String, qualifier: String): Long = 
        withContext(Dispatchers.IO) {
            val get = Get(Bytes.toBytes(key))
            get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier))
            
            val result = hbaseClient.get(get)
            if (result.isEmpty) 0L
            else Bytes.toLong(result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier)) ?: ByteArray(8) { 0 })
        }

    override suspend fun put(key: String, columnFamily: String, qualifier: String, value: Long) = 
        withContext(Dispatchers.IO) {
            val put = Put(Bytes.toBytes(key))
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier), Bytes.toBytes(value))
            hbaseClient.put(put)
        }

    override suspend fun delete(key: String, columnFamily: String, qualifier: String?) = 
        withContext(Dispatchers.IO) {
            val delete = Delete(Bytes.toBytes(key))
            if (qualifier != null) {
                delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier))
            }
            hbaseClient.delete(delete)
        }

    override suspend fun getRow(key: String, columnFamily: String): Map<String, ByteArray> = 
        withContext(Dispatchers.IO) {
            val get = Get(Bytes.toBytes(key))
            get.addFamily(Bytes.toBytes(columnFamily))
            
            val result = hbaseClient.get(get)
            if (result.isEmpty) return@withContext emptyMap()
            
            val rowData = mutableMapOf<String, ByteArray>()
            val familyMap = result.getFamilyMap(Bytes.toBytes(columnFamily))
            familyMap?.forEach { (qualifier, value) ->
                rowData[Bytes.toString(qualifier)] = value
            }
            rowData
        }

    override suspend fun batchGet(keys: List<String>, columnFamily: String, qualifier: String): Map<String, Long> = 
        withContext(Dispatchers.IO) {
            if (keys.isEmpty()) return@withContext emptyMap()
            
            val results = mutableMapOf<String, Long>()
            keys.forEach { key ->
                try {
                    val value = get(key, columnFamily, qualifier)
                    results[key] = value
                } catch (e: Exception) {
                    results[key] = 0L
                }
            }
            results
        }
}