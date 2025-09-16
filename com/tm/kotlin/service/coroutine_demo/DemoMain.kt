package com.tm.kotlin.service.coroutine_demo

import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(VRoute()) { result ->
        if (result.succeeded()) {
            println("VRoute Verticle deployed successfully")
        } else {
            println("Failed to deploy VRoute Verticle: ${result.cause()}")
        }
    }
}