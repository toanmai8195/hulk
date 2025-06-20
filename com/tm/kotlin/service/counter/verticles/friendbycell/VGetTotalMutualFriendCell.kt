package com.tm.kotlin.service.counter.verticles.friendbycell

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject


class VGetTotalMutualFriendCell @Inject constructor(
    private val hBaseClient: IHBaseClient,
    private val registry: PrometheusMeterRegistry,
) : AbstractVerticle() {
    private val getTotalMutualFriendsCounter: Counter = Counter.builder("total_mutual_friend_by_cell_counter")
        .description("Number of getMutualFriends cells")
        .register(registry)

    private val getTotalMutualFriendsTimer = Timer
        .builder("total_mutual_friend_by_cell_latency")
        .description("Latency of getMutualFriends cells")
        .register(registry)

    private val payloadSizeSummary: DistributionSummary = DistributionSummary
        .builder("total_mutual_friend_cell_payload_size_bytes")
        .description("Size of HBase payloads when getting mutual friends")
        .baseUnit("bytes")
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()

        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                getTotalMutualFriendsCounter.increment()
                getTotalMutualFriends(CELL_ACTOR, CELL_PARTNER)
            }
        }
    }

    private fun getTotalMutualFriends(actor: String, partner: String): Int {
        return getTotalMutualFriendsTimer.record<Int> {
            val actorFriendsRs = hBaseClient.get(Get(genRowKey(actor).toByteArray()))
            val partnerFriendsRs = hBaseClient.get(Get(genRowKey(partner).toByteArray()))

            val actorMap = actorFriendsRs.getFamilyMap("df".toByteArray())
            val partnerMap = partnerFriendsRs.getFamilyMap("df".toByteArray())

            val actorPayloadSize = actorMap.entries.sumOf { (k, v) -> k.size + v.size }
            val partnerPayloadSize = partnerMap.entries.sumOf { (k, v) -> k.size + v.size }

            payloadSizeSummary.record(actorPayloadSize.toDouble())
            payloadSizeSummary.record(partnerPayloadSize.toDouble())

            val actorFriends = actorMap
                .map { (qualifier, _) ->
                    Bytes.toString(qualifier)
                }.toList()

            val partnerFriends = partnerMap
                .map { (qualifier, _) ->
                    Bytes.toString(qualifier)
                }.toList()

            registry.summary("friend_size", "source", this::class.simpleName).record(actorFriends.size.toDouble())
            registry.summary("friend_size", "source", this::class.simpleName).record(partnerFriends.size.toDouble())

            actorFriends.intersect(partnerFriends).size
        }
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}