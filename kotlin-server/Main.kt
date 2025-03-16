package com.tm

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

class KotlinServerVertical : AbstractVerticle() {
    override fun start() {
        val router = Router.router(vertx)
        
        router.get("/").handler { ctx ->
            ctx.response()
                .putHeader("content-type", "text/plain")
                .end("Hello, World!")
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080) { http ->
                if (http.succeeded()) {
                    println("HTTP server started on port 8080")
                } else {
                    println("Failed to start HTTP server: ${http.cause().message}")
                }
            }
    }
}

fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(KotlinServerVertical())
}
