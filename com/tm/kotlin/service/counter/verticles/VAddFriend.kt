package com.tm.kotlin.service.counter.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.common.utils.PhoneUtils
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.inject.Inject


class VAddFriend @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
//        addFriends(
//            "01666621555", listOf(
//                "01666621554",
//                "01666621556",
//                "01666621557",
//                "01666621558"
//            )
//        )
//
//        addFriends(
//            "01666621556", listOf(
//                "01666621555",
//                "01666621557",
//                "01666621558",
//                "01666621559",
//            )
//        )

        addFriends(
            "01666621558", (1..5000).map {
                PhoneUtils.generateVietnamPhoneNumber()
            }
        )
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
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}