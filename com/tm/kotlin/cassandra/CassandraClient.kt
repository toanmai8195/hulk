package com.tm.kotlin.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CassandraClient @Inject constructor(

) {
    val session: CqlSession = CqlSession.builder()
        .addContactPoint(InetSocketAddress("127.0.0.1", 9042))
        .withLocalDatacenter("datacenter1")
        .build()

    fun execute(sql: String): ResultSet {
        return session.execute(sql)
    }
}