package com.tm.kotlin.service.counter.verticles.friendbycell

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import javax.inject.Inject

class VGetTotalFriendByCell @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val getTotalFriendsCounter: Counter = Counter.builder("total_friend_by_cell_counter")
        .description("Number of getTotalFriends calls")
        .register(registry)

    private val getTotalFriendsTimer = Timer
        .builder("total_friend_by_cell_latency")
        .description("Latency of getTotalFriends calls")
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getTotalFriendsCounter.increment()
                getTotalFriends(CELL_ACTOR)
            }
        }
    }

    private fun getTotalFriends(actor: String): Int {
        return getTotalFriendsTimer.record<Int> {
            val get = Get(genRowKey(actor).toByteArray())
            val rs = hBaseClient.get(get)
            val total = rs.getFamilyMap("df".toByteArray()).size
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