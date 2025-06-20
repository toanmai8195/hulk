package com.tm.kotlin.service.counter.verticles.friendbycell

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject

class VGetMutualFriendCell @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val getMutualFriendsCounter: Counter = Counter.builder("mutual_friend_by_cell_counter")
        .description("Number of getMutualFriends cells")
        .register(registry)

    private val getMutualFriendsTimer = Timer
        .builder("mutual_friend_by_cell_latency")
        .description("Latency of getMutualFriends cells")
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getMutualFriendsCounter.increment()
                getMutualFriends(CELL_ACTOR_10K, CELL_PARTNER_10K)
            }
        }
    }

    private fun getMutualFriends(actor: String, partner: String): Set<String> {
        return getMutualFriendsTimer.record<Set<String>> {
            val actorFriendsRs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))
            val partnerFriendsRs = hBaseClient.get(Get(genRowKey(partner).toByteArray()))

            val actorFriends = actorFriendsRs.getFamilyMap("df".toByteArray())
                .map { (qualifier, _) ->
                    Bytes.toString(qualifier)
                }.toList()

            val partnerFriends = partnerFriendsRs.getFamilyMap("df".toByteArray())
                .map { (qualifier, _) ->
                    Bytes.toString(qualifier)
                }.toList()

            registry.summary("friend_size", "source", this::class.simpleName).record(actorFriends.size.toDouble())
            registry.summary("friend_size", "source", this::class.simpleName).record(partnerFriends.size.toDouble())

            actorFriends.intersect(partnerFriends)
        }
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}