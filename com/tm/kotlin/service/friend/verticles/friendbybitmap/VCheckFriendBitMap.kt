package com.tm.kotlin.service.friend.verticles.friendbybitmap

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.friend.CounterMain
import com.tm.kotlin.service.friend.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.friend.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.inject.Inject

class VCheckFriendBitMap @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val counterMetric: Counter = Counter.builder("check_friend_by_bitmap_counter")
        .description("Number of check friend calls")
        .register(registry)

    private val timerMetric = Timer
        .builder("check_friend_by_bitmap_latency")
        .description("Latency of check friend calls")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                counterMetric.increment()
                val randomPhone = "0101000" + String.format("%04d", (0..7000).random())
                println("BITMAP: $BITMAP_ACTOR & $randomPhone=${checkFriend(BITMAP_ACTOR, randomPhone)}")
            }
        }
    }

    private fun checkFriend(actor: String, partner: String): Boolean {
        return timerMetric.record<Boolean> {
            val actorFriendsRs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))

            val bitmapOfActor = actorFriendsRs.getValue(
                "df".toByteArray(),
                "bitmap".toByteArray()
            ).toBitmap()

            registry.summary("friend_size", "source", this::class.simpleName).record(bitmapOfActor.count().toDouble())

            bitmapOfActor.contains(partner.removePrefix("0").toInt())
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