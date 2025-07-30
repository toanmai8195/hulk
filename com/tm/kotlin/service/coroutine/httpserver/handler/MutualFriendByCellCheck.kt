package com.tm.kotlin.service.coroutine.httpserver.handler

import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseClient
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.getRow
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.toStringValue
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject

class MutualFriendByCellCheck @Inject constructor(
    private val hbaseClient: HBaseClient,
) : IApiHandler {
    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val actor = requestBody.getString("actor")
        val partner = requestBody.getString("partner")

        println("[Cell] Actor=${actor} Partner=${partner}")

        val actorGet = Get(Bytes.toBytes(actor.getRow()))
        val partnerGet = Get(Bytes.toBytes(partner.getRow()))

        val results = hbaseClient.gets(listOf(actorGet, partnerGet)) { rss ->
            return@gets rss.mapNotNull { rs ->
                if (!rs.isEmpty) {
                    rs.row.toStringValue()
                        .reversed() to rs.getFamilyMap("df".toByteArray()).keys.map { it.toStringValue() }.filter { it.length == 11 }
                } else {
                    null
                }
            }.toMap()
        }

        return JsonObject()
            .put("actor", actor)
            .put("partner", partner)
            .put(
                "mutualTotal",
                (results[actor] ?: listOf()).intersect(results[partner] ?: listOf()).count()
            )
    }
}