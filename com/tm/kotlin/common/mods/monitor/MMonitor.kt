package com.tm.kotlin.common.mods.monitor

import com.tm.kotlin.common.http.API
import com.tm.kotlin.common.http.HttpFactory
import com.tm.kotlin.common.mods.base.MBase
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.vertx.core.Verticle
import io.vertx.core.http.HttpMethod

@Module(
    includes = [
        MBase::class
    ]
)
class MMonitor {
    @Provides
    fun providePrometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Provides
    @IntoMap
    @StringKey(value = "Monitor")
    fun provideHelloServiceVerticle(
        httpFactory: HttpFactory,
        registry: PrometheusMeterRegistry
    ): Verticle {
        return httpFactory.create(
            "Monitor",
            8088,
            listOf(
                API(
                    HttpMethod.GET,
                    "/metrics"
                ) { ctx ->
                    ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end(registry.scrape())
                }
            )
        )
    }
}