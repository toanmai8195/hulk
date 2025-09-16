package com.tm.kotlin.service.coroutine_demo

import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

class VRoute : CoroutineVerticle() {
    private val handler = Handler()

    override suspend fun start() {
        val router = createRouter()
        val serverOptions = HttpServerOptions().setPort(8080)

        vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .listen(8080)
            .coAwait()

        println("VRoute Verticle started on port 8080")
        println("Test APIs:")
        println("- With coroutine: GET http://localhost:8080/api/test-with-coroutine")
        println("- Without coroutine: GET http://localhost:8080/api/test-without-coroutine")
    }

    private fun createRouter(): Router {
        val router = Router.router(vertx)

        router.get("/api/test-with-coroutine").handler { context ->
            println("/api/test-with-coroutine - Thread=${Thread.currentThread().name}")
            launch(context.vertx().dispatcher()) {
                handler.blockingHandlerWithCoroutine(context)
            }
        }

        router.get("/api/test-without-coroutine").handler { context ->
            println("/api/test-without-coroutine - Thread=${Thread.currentThread().name}")
            handler.blockingHandlerWithoutCoroutine(context)
        }

        return router
    }
}