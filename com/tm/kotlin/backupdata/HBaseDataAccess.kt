package com.tm.kotlin.backupdata

import com.tm.kotlin.common.hbase.HBaseClient
import com.tm.kotlin.common.hbase.HBaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HBaseDataAccess @Inject constructor(
    private val hbaseFactory: HBaseFactory
) {
    private val tableName = "hulk:backup_data"
    private lateinit var client: HBaseClient

    suspend fun init() = withContext(Dispatchers.IO) {
        client = hbaseFactory.create(tableName)
    }

    suspend fun getData(rowKey: String): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val get = Get(Bytes.toBytes(rowKey))
            val result: Result = client.get(get)
            
            if (result.isEmpty) {
                return@withContext null
            }
            
            val data = mutableMapOf<String, String>()
            val value = result.getValue(Bytes.toBytes("cf"), Bytes.toBytes("data"))
            if (value != null) {
                data["data"] = Bytes.toString(value)
            }
            data
        } catch (e: Exception) {
            throw DataAccessException("Failed to read from HBase: ${e.message}", e)
        }
    }

    suspend fun scanData(startRow: String?, stopRow: String?): List<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val scan = Scan()
            startRow?.let { scan.withStartRow(Bytes.toBytes(it)) }
            stopRow?.let { scan.withStopRow(Bytes.toBytes(it)) }
            
            val scanner = client.scan(scan)
            val results = mutableListOf<Map<String, String>>()
            
            scanner.use { 
                for (result in it) {
                    val data = mutableMapOf<String, String>()
                    data["rowKey"] = Bytes.toString(result.row)
                    
                    val value = result.getValue(Bytes.toBytes("cf"), Bytes.toBytes("data"))
                    if (value != null) {
                        data["data"] = Bytes.toString(value)
                    }
                    results.add(data)
                }
            }
            results
        } catch (e: Exception) {
            throw DataAccessException("Failed to scan HBase: ${e.message}", e)
        }
    }
}