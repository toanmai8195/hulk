package com.tm.kotlin.common.http

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class VHttp @AssistedInject constructor(
    private val vertx: Vertx,
    private val router: Router,
    @Assisted private val serviceName: String,
    @Assisted private val port: Int,
    @Assisted private val apis: List<API>
) : AbstractVerticle() {
    @AssistedFactory
    interface Factory : HttpFactory

    override fun start(startPromise: Promise<Void>) {
        router.route().handler(BodyHandler.create())

        apis.forEach { api ->
            addApi(api)
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port) { result ->
                if (result.succeeded()) {
                    println("🚀 Service $serviceName running on http://localhost:$port")
                    startPromise.complete()
                } else {
                    startPromise.fail(result.cause())
                }
            }
    }

    fun addApi(api: API) {
        when (api.httpMethod) {
            HttpMethod.GET -> {
                router.get(api.path).handler(api.handler)
            }

            HttpMethod.POST -> {
                router.post(api.path).handler(api.handler)
            }

            else -> {
                println("Method not allow!")
            }
        }
    }

    override fun stop(stopPromise: Promise<Void?>?) {
        println("VHttp stopped!")
    }
}