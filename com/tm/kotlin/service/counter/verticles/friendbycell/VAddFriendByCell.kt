package com.tm.kotlin.service.counter.verticles.friendbycell

import com.tm.kotlin.common.hbase.IHBaseClient
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_ACTOR_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.BITMAP_PARTNER_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_ACTOR_10K
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER
import com.tm.kotlin.service.counter.CounterMain.Companion.CELL_PARTNER_10K
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Put
import javax.inject.Inject


class VAddFriendByCell @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()

        addFriends(
            CELL_ACTOR,
            (10000000..10005000).map {
                "010$it"
            }
        )

        addFriends(
            CELL_PARTNER,
            (10002500..10007500).map {
                "010$it"
            }
        )

        addFriends(
            CELL_ACTOR_10K,
            (10000000..10010000).map {
                "010$it"
            }
        )

        addFriends(
            CELL_PARTNER_10K,
            (10005000..10015000).map {
                "010$it"
            }
        )

        println("Insert cell done!")
    }

    private fun addFriends(actor: String, friendIds: List<String>) {
        val put = Put(
            genRowKey(actor).toByteArray()
        )


        friendIds.forEach {
            put.addColumn(
                "df".toByteArray(),
                it.toByteArray(),
                "".toByteArray()
            )
        }

        hBaseClient.put(put)
    }


    private fun genRowKey(actor: String): String {
        return actor.reversed()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}