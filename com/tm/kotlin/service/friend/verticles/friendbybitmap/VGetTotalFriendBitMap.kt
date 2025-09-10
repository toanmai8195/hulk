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
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject

class VGetTotalFriendBitMap @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val getTotalFriendsCounter: Counter = Counter.builder("total_friend_by_bitmap_counter")
        .description("Number of getTotalFriends calls")
        .register(registry)

    private val getTotalFriendsTimer = Timer
        .builder("total_friend_by_bitmap_latency")
        .description("Latency of getTotalFriends calls")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getTotalFriendsCounter.increment()
                getTotalFriends(BITMAP_ACTOR)
            }
        }
    }

    private fun getTotalFriends(actor: String): Int {
        return getTotalFriendsTimer.record<Int> {
            val rs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))
            val total = Bytes.toInt(rs.getValue("df".toByteArray(), "total".toByteArray()))
            registry.summary("friend_size", "source", this::class.simpleName).record(total.toDouble())
            total
        }
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}