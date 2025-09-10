package com.tm.kotlin.service.friend.verticles.friendbybitmap

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.friend.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.friend.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.inject.Inject

class VSizeBitmapTest @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {

    override fun start(startPromise: Promise<Void>) {
        val bitmapList = mutableListOf<RoaringBitmap>()

        Gauge.builder("test_size_bitmap_list", bitmapList) { it.size.toDouble() }
            .description("Current size of bitmap list")
            .register(registry)

        startPromise.complete()
        val actorFriendsRs = hBaseClient.get(Get(genRowKey(BITMAP_ACTOR).toByteArray()))

        val bitmapOfActor = actorFriendsRs.getValue(
            "df".toByteArray(),
            "bitmap".toByteArray()
        ).toBitmap()

        vertx.setPeriodic((1000 / RPS).toLong()) {
            println("Size=${bitmapList.size}")
            bitmapList.add(bitmapOfActor)
        }
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