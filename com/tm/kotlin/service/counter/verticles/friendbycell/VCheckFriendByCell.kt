package com.tm.kotlin.service.counter.verticles.friendbycell

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.RPS
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.util.Bytes
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

class VCheckFriendByCell @Inject constructor(
    private val hBaseClient: IHBaseClient,
    registry: PrometheusMeterRegistry
) : AbstractVerticle() {
    private val counterMetric: Counter = Counter.builder("check_friend_by_cell_counter")
        .description("Number of getTotalFriends calls")
        .register(registry)

    private val timerMetric = Timer
        .builder("check_friend_by_cell_latency")
        .description("Latency of getTotalFriends calls")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
        vertx.setPeriodic((1000 / RPS).toLong()) {
            CounterMain.executor.execute {
                counterMetric.increment()
                val randomPhone = "0101000" + String.format("%04d", (0..7000).random())
                println("CELL: $CELL_ACTOR & $randomPhone=${checkFriend(CELL_ACTOR, randomPhone)}")
            }
        }
    }

    private fun checkFriend(actor: String, friendID: String): Boolean {
        return timerMetric.record<Boolean> {
            val get = Get(genRowKey(actor).toByteArray())
            val rs = hBaseClient.get(get)

            return@record rs.getFamilyMap("df".toByteArray())
                .map { (qualifier, _) ->
                    Bytes.toString(qualifier)
                }.toList().contains(friendID)
        }
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}