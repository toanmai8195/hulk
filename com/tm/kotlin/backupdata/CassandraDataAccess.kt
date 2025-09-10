package com.tm.kotlin.backupdata

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CassandraDataAccess @Inject constructor() {
    private lateinit var session: CqlSession
    private val keyspace = "backup_data"
    private val table = "data_table"

    suspend fun init() = withContext(Dispatchers.IO) {
        session = CqlSession.builder()
            .addContactPoint(InetSocketAddress("localhost", 9042))
            .withLocalDatacenter("datacenter1")
            .build()
        
        createKeyspaceAndTable()
    }

    private suspend fun createKeyspaceAndTable() = withContext(Dispatchers.IO) {
        try {
            session.execute(
                """
                CREATE KEYSPACE IF NOT EXISTS $keyspace
                WITH REPLICATION = {
                    'class': 'SimpleStrategy',
                    'replication_factor': 1
                }
                """.trimIndent()
            )
            
            session.execute("USE $keyspace")
            
            session.execute(
                """
                CREATE TABLE IF NOT EXISTS $table (
                    row_key TEXT PRIMARY KEY,
                    data_json TEXT
                )
                """.trimIndent()
            )
        } catch (e: Exception) {
            throw DataAccessException("Failed to initialize Cassandra: ${e.message}", e)
        }
    }

    suspend fun getData(rowKey: String): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val statement = session.prepare("SELECT data_json FROM $keyspace.$table WHERE row_key = ?")
            val result = session.execute(statement.bind(rowKey))
            val row: Row? = result.one()
            
            row?.let {
                val jsonData = it.getString("data_json")
                parseJsonToMap(jsonData ?: "")
            }
        } catch (e: Exception) {
            throw DataAccessException("Failed to read from Cassandra: ${e.message}", e)
        }
    }

    suspend fun scanData(startRow: String?, stopRow: String?): List<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val query = if (startRow != null && stopRow != null) {
                "SELECT row_key, data_json FROM $keyspace.$table WHERE row_key >= ? AND row_key < ? ALLOW FILTERING"
            } else {
                "SELECT row_key, data_json FROM $keyspace.$table LIMIT 1000"
            }
            
            val statement = session.prepare(query)
            val result = if (startRow != null && stopRow != null) {
                session.execute(statement.bind(startRow, stopRow))
            } else {
                session.execute(statement.bind())
            }
            
            val results = mutableListOf<Map<String, String>>()
            for (row in result) {
                val data = mutableMapOf<String, String>()
                data["rowKey"] = row.getString("row_key") ?: ""
                
                val jsonData = row.getString("data_json")
                data.putAll(parseJsonToMap(jsonData ?: ""))
                results.add(data)
            }
            results
        } catch (e: Exception) {
            throw DataAccessException("Failed to scan Cassandra: ${e.message}", e)
        }
    }

    private fun parseJsonToMap(json: String): Map<String, String> {
        if (json.isEmpty()) return emptyMap()
        
        try {
            val map = mutableMapOf<String, String>()
            val cleanJson = json.trim('{', '}')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").forEach { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim().trim('"')
                        val value = parts[1].trim().trim('"')
                        map[key] = value
                    }
                }
            }
            return map
        } catch (e: Exception) {
            return emptyMap()
        }
    }
}