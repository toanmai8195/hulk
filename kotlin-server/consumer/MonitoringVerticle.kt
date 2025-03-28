package com.tm.consumer

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

class MonitoringVerticle : AbstractVerticle() {
    companion object {
        val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    override fun start() {
        val router = Router.router(vertx)
        router.get("/metrics").handler { rc ->
            rc.response().putHeader("Content-Type", "text/plain").end(registry.scrape())
        }

        vertx.createHttpServer().requestHandler(router).listen(8080)
    }
}