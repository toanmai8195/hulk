package com.tm.kotlin.service.counter.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.inject.Inject


class VGetFriend @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        println("List friend ${getFriends("01666621555")}")
        println("List friend ${getFriends("01666621556")}")
        println("List friend ${getFriends("01666621557")}")
        println("List friend ${getFriends("01666621558")}")
    }

    private fun getFriends(actor: String): Pair<Int, List<String>> {
        val rs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))

        return Bytes.toInt(rs.getValue("df".toByteArray(), "total".toByteArray())) to
                rs.getValue(
                    "df".toByteArray(),
                    "bitmap".toByteArray()
                ).toBitmap()
                    .toListString()
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

    private fun RoaringBitmap.toListString(): List<String> {
        return this.toList().map {
            "0$it"
        }
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}