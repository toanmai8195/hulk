package com.tm.kotlin.service.coroutine.httpserver.handler

import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseClient
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.getRow
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.toBitmap
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.toStringValue
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import javax.inject.Inject

class MutualFriendCheck @Inject constructor(
    private val hbaseClient: HBaseClient,
) : IApiHandler {
    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val actor = requestBody.getString("actor")
        val partner = requestBody.getString("partner")

        println("Actor=${actor} Partner=${partner}")

        val actorGet = Get(Bytes.toBytes(actor.getRow()))
        val partnerGet = Get(Bytes.toBytes(partner.getRow()))

        val results = hbaseClient.gets(listOf(actorGet, partnerGet)) { rss ->
            return@gets rss.mapNotNull { rs ->
                if (!rs.isEmpty) {
                    rs.row.toStringValue().reversed() to rs.toBitMap()
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
                (RoaringBitmap.and(results[actor] ?: RoaringBitmap(), results[partner] ?: RoaringBitmap()).count())
            )
    }

    private fun Result.toBitMap(): RoaringBitmap {
        if (this.isEmpty) return RoaringBitmap()

        return this.getValue(
            "df".toByteArray(),
            "bitmap".toByteArray()
        ).toBitmap()
    }
}