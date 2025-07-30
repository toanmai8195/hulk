package com.tm.kotlin.service.coroutine.httpserver

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.service.coroutine.httpserver.verticles.HttpServerVerticle
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module(
    includes = [
        MBase::class,
        MMonitor::class
    ]
)
class HttpServerModule {
    @Provides
    @IntoMap
    @StringKey(value = "HttpServerVerticle")
    fun provideHttpVerticle(verticle: HttpServerVerticle): Verticle {
        return verticle
    }
}