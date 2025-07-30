package com.tm.kotlin.service.coroutine.httpserver.handler

import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseClient
import com.tm.kotlin.service.coroutine.httpserver.hbase.HBaseUtils.getRow
import io.vertx.core.json.JsonObject
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.inject.Inject

class FriendGenerate @Inject constructor(
    private val hbaseClient: HBaseClient
) : IApiHandler {
    companion object {
        const val ACTOR_PREFIX = "0166000"
        const val PARTNER_PREFIX = "0166000"
    }

    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val total = requestBody.getInteger("total") ?: 1

        (1..total).forEach { index ->
            addFriends(
                ACTOR_PREFIX + String.format("%04d", index),
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
            .map { PARTNER_PREFIX + String.format("%04d", it) }
    }

    private suspend fun addFriends(actor: String, friendIds: List<String>) {
        println("Add actor=${actor}")
        val friendsToBitmap = RoaringBitmap()
        friendIds.map {
            friendsToBitmap.add(it.removePrefix("0").toInt())
        }

        val put = Put(
            actor.getRow().toByteArray()
        ).addColumn(
            "df".toByteArray(),
            "bitmap".toByteArray(),
            friendsToBitmap.toBytes()
        ).addColumn(
            "df".toByteArray(),
            "total".toByteArray(),
            Bytes.toBytes(friendsToBitmap.count())
        )

        hbaseClient.put(put)
    }

    fun RoaringBitmap.toBytes(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)
        this.serialize(dataOutputStream)
        return byteArrayOutputStream.toByteArray()
        Int.MAX_VALUE
    }
}