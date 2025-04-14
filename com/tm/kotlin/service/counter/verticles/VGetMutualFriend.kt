package com.tm.kotlin.service.counter.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.inject.Inject


class VGetMutualFriend @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        println("01666621555 and 01666621556 = ${getMutualFriends("01666621555", "01666621556")}")
        val start = System.currentTimeMillis()
        println("01666621557 and 01666621558 = ${getMutualFriends("01666621557", "01666621557")}")
        println("Time=${System.currentTimeMillis() - start}")
    }

    private fun getMutualFriends(actor: String, partner: String): Int {
        val actorFriendsRs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))
        val partnerFriendsRs = hBaseClient.get(Get(genRowKey(partner).toByteArray()))

        val bitmapOfActor = actorFriendsRs.getValue(
            "df".toByteArray(),
            "bitmap".toByteArray()
        ).toBitmap()

        val bitmapOfPartner = partnerFriendsRs.getValue(
            "df".toByteArray(),
            "bitmap".toByteArray()
        ).toBitmap()

        val rs = RoaringBitmap.and(bitmapOfActor, bitmapOfPartner)

        return rs.count()
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    private fun ByteArray.toBitmap(): RoaringBitmap {
        val byteArrayInputStream = ByteArrayInputStream(this)
        val dataInputStream = DataInputStream(byteArrayInputStream)
        val bitmap = RoaringBitmap()
        bitmap.deserialize(dataInputStream)
        return bitmap
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}