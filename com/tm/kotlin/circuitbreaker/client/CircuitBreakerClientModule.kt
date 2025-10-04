package com.tm.kotlin.circuitbreaker.client

import com.tm.kotlin.common.mods.base.MBase
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module(includes = [MBase::class])
class CircuitBreakerClientModule {
    @Provides
    @IntoMap
    @StringKey(value = "CircuitBreakerClientVerticle")
    fun provideCircuitBreakerClientVerticle(verticle: CircuitBreakerClientVerticle): Verticle {
        return verticle
    }
}
