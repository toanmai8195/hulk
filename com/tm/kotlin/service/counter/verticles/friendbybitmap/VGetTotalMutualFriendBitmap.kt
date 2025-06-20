package com.tm.kotlin.service.counter.verticles.friendbybitmap

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.roaringbitmap.RoaringBitmap
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.inject.Inject

class VGetTotalMutualFriendBitmap @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val getTotalMutualFriendsCounter: Counter = Counter.builder("total_mutual_friend_by_bitmap_counter")
        .description("Number of getMutualFriends calls")
        .register(registry)

    private val getTotalMutualFriendsTimer = Timer
        .builder("total_mutual_friend_by_bitmap_latency")
        .description("Latency of getMutualFriends calls")
        .register(registry)

    private val payloadSizeSummary: DistributionSummary = DistributionSummary
        .builder("total_mutual_friend_bitmap_payload_size_bytes")
        .description("Size of HBase payloads when getting mutual friends")
        .baseUnit("bytes")
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getTotalMutualFriendsCounter.increment()
                getTotalMutualFriends(BITMAP_ACTOR, BITMAP_PARTNER)
            }
        }
    }

    private fun getTotalMutualFriends(actor: String, partner: String): Int {
        return getTotalMutualFriendsTimer.record<Int> {
            val actorFriendsRs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))
            val partnerFriendsRs = hBaseClient.get(Get(genRowKey(partner).toByteArray()))

            val actorBytes = actorFriendsRs.getValue("df".toByteArray(), "bitmap".toByteArray())
            val partnerBytes = partnerFriendsRs.getValue("df".toByteArray(), "bitmap".toByteArray())

            val bitmapOfActor = actorBytes.toBitmap()
            val bitmapOfPartner = partnerBytes.toBitmap()

            payloadSizeSummary.record(actorBytes.size.toDouble())
            payloadSizeSummary.record(partnerBytes.size.toDouble())

            registry.summary("friend_size", "source", this::class.simpleName).record(bitmapOfActor.count().toDouble())
            registry.summary("friend_size", "source", this::class.simpleName).record(bitmapOfPartner.count().toDouble())

            val rs = RoaringBitmap.and(bitmapOfActor, bitmapOfPartner)
            rs.count()
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