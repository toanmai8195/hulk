package com.tm.kotlin.circuitbreaker.server

import com.tm.kotlin.common.mods.base.MBase
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module(includes = [MBase::class])
class CircuitBreakerServerModule {
    @Provides
    @IntoMap
    @StringKey(value = "CircuitBreakerServerVerticle")
    fun provideCircuitBreakerServerVerticle(verticle: CircuitBreakerServerVerticle): Verticle {
        return verticle
    }
}
