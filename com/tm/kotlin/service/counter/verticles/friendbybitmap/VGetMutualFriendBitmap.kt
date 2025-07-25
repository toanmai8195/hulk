package com.tm.kotlin.service.counter.verticles.friendbybitmap

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
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

class VGetMutualFriendBitmap @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val getMutualFriendsCounter: Counter = Counter.builder("mutual_friend_by_bitmap_counter")
        .description("Number of getMutualFriends calls")
        .register(registry)

    private val getMutualFriendsTimer = Timer
        .builder("mutual_friend_by_bitmap_latency")
        .description("Latency of getMutualFriends calls")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()

        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getMutualFriendsCounter.increment()
                getMutualFriends(BITMAP_ACTOR_10K, BITMAP_PARTNER_10K)
            }
        }
    }

    private fun getMutualFriends(actor: String, partner: String): Set<Int> {
        return getMutualFriendsTimer.record<Set<Int>> {
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

            registry.summary("friend_size", "source", this::class.simpleName).record(bitmapOfActor.count().toDouble())
            registry.summary("friend_size", "source", this::class.simpleName).record(bitmapOfPartner.count().toDouble())

            val rs = RoaringBitmap.and(bitmapOfActor, bitmapOfPartner)
            rs.toSet()
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