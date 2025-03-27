package com.tm.consumer

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise

class LowPriorityWorkerVertical() : AbstractVerticle() {
    override fun start(startPromise: Promise<Void?>?) {
        println("low_priority_worker listening...")

        vertx.eventBus().consumer<String>("low_priority_worker") { event ->
            println("[Low] Message: ${event.body()}")
            event.reply(null)
        }
        startPromise?.complete()
    }

    override fun stop(stopPromise: Promise<Void?>?) {
        super.stop(stopPromise)
    }

}