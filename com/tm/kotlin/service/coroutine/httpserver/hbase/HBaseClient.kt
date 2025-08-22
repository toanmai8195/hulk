package com.tm.kotlin.service.coroutine.httpserver.hbase

import com.tm.kotlin.common.error.ErrorCode
import com.tm.kotlin.common.error.HulkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Table
import javax.inject.Inject
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.ResultScanner
import org.apache.hadoop.hbase.client.Scan

class HBaseClient @Inject constructor() {
    companion object {
        private const val TABLE_NAME = "hulk:com.tm.counter"
    }

    private var connection: Connection
    private var table: Table

    init {
        val config: Configuration = HBaseConfiguration.create()
        config.set("hbase.zookeeper.quorum", "hbase")
        config.set("hbase.zookeeper.property.clientPort", "2181")
        connection = ConnectionFactory.createConnection(config)
        table = connection.getTable(TableName.valueOf(TABLE_NAME))
    }


    suspend fun put(put: Put) = withContext(Dispatchers.IO) {
        try {
            table.put(put)
        } catch (ex: Exception) {
            throw HulkException(
                ErrorCode.DB_ERROR,
                "Put data to hbase failed!",
                ex
            )
        }
    }

    suspend fun <T> get(get: Get, converter: (Result) -> T): T = withContext(Dispatchers.IO) {
        try {
            val result = table.get(get)
            converter(result)
        } catch (ex: Exception) {
            throw HulkException(
                ErrorCode.DB_ERROR,
                "Get data from hbase failed!",
                ex
            )
        }
    }

    suspend fun <T> gets(gets: List<Get>, converter: (Array<Result>) -> Map<String, T>): Map<String, T> =
        withContext(Dispatchers.IO) {
            try {
                val results = table.get(gets)
                converter(results)
            } catch (ex: Exception) {
                throw HulkException(
                    ErrorCode.DB_ERROR,
                    "Gets data from hbase failed!",
                    ex
                )
            }
        }

    suspend fun delete(delete: Delete) = withContext(Dispatchers.IO) {
        try {
            table.delete(delete)
        } catch (ex: Exception) {
            throw HulkException(
                ErrorCode.DB_ERROR,
                "Delete failed!",
                ex
            )
        }
    }

    suspend fun <T> scan(scan: Scan, converter: (ResultScanner) -> List<T>): List<T> = withContext(Dispatchers.IO) {
        try {
            table.getScanner(scan).use { scanner ->
                converter(scanner)
            }
        } catch (ex: Exception) {
            throw HulkException(
                ErrorCode.DB_ERROR,
                "Scan data from hbase failed!",
                ex
            )
        }
    }
}