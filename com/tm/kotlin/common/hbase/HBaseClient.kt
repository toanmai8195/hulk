package com.tm.kotlin.common.hbase

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.*

class HBaseClient @AssistedInject constructor(
    config: Configuration,
    @Assisted private val tableName: String
) : IHBaseClient {
    companion object {
        private const val LIMIT_COLUMNS_PER_FAMILY = 1000
        private const val LIMIT_ROWS_PER_SCAN = 100
    }

    val connection: Connection = ConnectionFactory.createConnection(config)
    private val table = connection.getTable(TableName.valueOf(tableName))

    @AssistedFactory
    interface Factory : HBaseFactory

    override fun put(put: Put) {
        put.ttl
        table.put(put)
        return
    }

    override fun get(get: Get): Result {
        get.readVersions(1)
        get.maxResultsPerColumnFamily = LIMIT_COLUMNS_PER_FAMILY
        return table.get(get)
    }

    override fun scan(scan: Scan): ResultScanner {
        scan.readVersions(1)
        scan.maxResultsPerColumnFamily = LIMIT_COLUMNS_PER_FAMILY
        scan.limit = LIMIT_ROWS_PER_SCAN
        return table.getScanner(scan)
    }

    override fun delete(delete: Delete) {
        table.delete(delete)
    }

    override fun inc(increment: Increment): Result {
        return table.increment(increment)
    }
}

interface HBaseFactory {
    fun create(tableName: String): HBaseClient
}