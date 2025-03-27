package com.tm.consumer

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise

class HighPriorityWorkerVertical() : AbstractVerticle() {
    override fun start(startPromise: Promise<Void?>?) {
        println("high_priority_worker listening...")
        vertx.eventBus().consumer<String>("high_priority_worker") { event ->
            println("[High] Message: ${event.body()}")
            event.reply(null)
        }
        startPromise?.complete()
    }

    override fun stop(stopPromise: Promise<Void?>?) {
        super.stop(stopPromise)
    }
}