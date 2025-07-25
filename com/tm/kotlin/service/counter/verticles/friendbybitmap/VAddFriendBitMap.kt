package com.tm.kotlin.service.counter.verticles.friendbybitmap

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER_10K
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.inject.Inject


class VAddFriendBitMap @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()

        addFriends(
            BITMAP_ACTOR,
            (10000000..10005000).map {
                "010$it"
            }
        )

        addFriends(
            BITMAP_PARTNER,
            (10002500..10007500).map {
                "010$it"
            }
        )

        addFriends(
            BITMAP_ACTOR_10K,
            (10000000..10010000).map {
                "010$it"
            }
        )

        addFriends(
            BITMAP_PARTNER_10K,
            (10005000..10015000).map {
                "010$it"
            }
        )
        println("Insert bitmap done!")
    }

    private fun addFriends(actor: String, friendIds: List<String>) {
        val friendsToBitmap = RoaringBitmap()
        friendIds.map {
            friendsToBitmap.add(it.removePrefix("0").toInt())
        }

        val put = Put(
            genRowKey(actor).toByteArray()
        ).addColumn(
            "df".toByteArray(),
            "bitmap".toByteArray(),
            friendsToBitmap.toBytes()
        ).addColumn(
            "df".toByteArray(),
            "total".toByteArray(),
            Bytes.toBytes(friendsToBitmap.count())
        )

        hBaseClient.put(put)
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    fun RoaringBitmap.toBytes(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)
        this.serialize(dataOutputStream)
        return byteArrayOutputStream.toByteArray()
        Int.MAX_VALUE
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}