package com.tm.kotlin.service.openapi

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
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
class OpenApiModule {
    @Provides
    @IntoMap
    @StringKey(value = "HttpServerVerticle")
    fun provideHttpVerticle(verticle: HttpServerVerticle): Verticle {
        return verticle
    }
}