package com.tm.kotlin.service.coroutine.httpserver.handler

import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseClient
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.getRow
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject

class FriendByCellGenerate @Inject constructor(
    private val hbaseClient: HBaseClient
) : IApiHandler {
    companion object {
        const val ACTOR_BY_CELL_PREFIX = "0165000"
        const val PARTNER_BY_CELL_PREFIX = "0165000"
    }

    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val total = requestBody.getInteger("total") ?: 1

        (1..total).forEach { index ->
            addFriends(
                ACTOR_BY_CELL_PREFIX + String.format("%04d", index),
                genFriendIds()
            )
        }

        return JsonObject()
            .put("status", "DONE")

    }


    private fun genFriendIds(): List<String> {
        return (0..9999)
            .shuffled()
            .take(5000)
            .map { PARTNER_BY_CELL_PREFIX + String.format("%04d", it) }
    }

    private suspend fun addFriends(actor: String, friendIds: List<String>) {
        println("Add actor=${actor}")

        val put = Put(
            actor.getRow().toByteArray()
        )

        friendIds.forEach { friendId ->
            put.addColumn(
                "df".toByteArray(),
                friendId.toByteArray(),
                Bytes.toBytes(1)
            )
        }

        hbaseClient.put(put)
    }
}