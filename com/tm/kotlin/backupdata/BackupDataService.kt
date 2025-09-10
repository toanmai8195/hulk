package com.tm.kotlin.backupdata

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupDataService @Inject constructor(
    private val hbaseDataAccess: HBaseDataAccess,
    private val cassandraDataAccess: CassandraDataAccess,
    private val circuitBreaker: CircuitBreaker
) {
    suspend fun init() = coroutineScope {
        launch { hbaseDataAccess.init() }
        launch { cassandraDataAccess.init() }
    }

    suspend fun getData(rowKey: String): Map<String, String>? {
        return try {
            circuitBreaker.execute {
                hbaseDataAccess.getData(rowKey)
            }
        } catch (e: Exception) {
            println("HBase failed, switching to Cassandra backup: ${e.message}")
            cassandraDataAccess.getData(rowKey)
        }
    }

    suspend fun scanData(startRow: String? = null, stopRow: String? = null): List<Map<String, String>> {
        return try {
            circuitBreaker.execute {
                hbaseDataAccess.scanData(startRow, stopRow)
            }
        } catch (e: Exception) {
            println("HBase failed, switching to Cassandra backup: ${e.message}")
            cassandraDataAccess.scanData(startRow, stopRow)
        }
    }
}