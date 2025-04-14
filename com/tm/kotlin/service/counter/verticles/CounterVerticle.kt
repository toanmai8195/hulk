package com.tm.kotlin.service.counter.verticles

import com.tm.kotlin.common.hbase.IHBaseClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import org.apache.hadoop.hbase.client.Increment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CounterVerticle @Inject constructor(
    private val hBaseClient: IHBaseClient
) : AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        val label = "label_test_1"
        Thread {
            count(label)
        }.start()
        startPromise.complete()
    }

    private fun count(label: String): Future<Unit> {
        val promise = Promise.promise<Unit>()
        Thread {
            (1..100).forEach {
                println("Start counter=${it}: $label in thread=${Thread.currentThread().name}")
                try {
                    hBaseClient.inc(
                        Increment(
                            label.toByteArray()
                        ).addColumn(
                            "df".toByteArray(),
                            "counter".toByteArray(),
                            1
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            promise.complete()
        }.start()
        return promise.future()
    }

    override fun stop(stopPromise: Promise<Void>) {
        println("Stop ${this.javaClass.simpleName}!!!")
    }
}