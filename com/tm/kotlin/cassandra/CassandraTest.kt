package com.tm.kotlin.cassandra

import dagger.Component
import javax.inject.Singleton

class CassandraTest {
}

@Singleton
@Component
interface AppComponent {
    val cassandraClient: CassandraClient
}

fun main() {
    val component = DaggerAppComponent.create()
    val client = component.cassandraClient
    val result = client.execute("SELECT release_version FROM system.local")
    println("Cassandra version: ${result.one()?.getString("release_version")}")
}