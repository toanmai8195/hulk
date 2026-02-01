package com.tm.kotlin.service.bitmap_loadtest

import com.tm.kotlin.service.bitmap_loadtest.verticles.BufferVerticle
import com.tm.kotlin.service.bitmap_loadtest.verticles.StreamVerticle
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module
class ServiceModule {
    @Provides
    @IntoMap
    @StringKey(value = "BufferVerticle")
    fun provideBufferVerticle(verticle: BufferVerticle): Verticle {
        return verticle
    }

    @Provides
    @IntoMap
    @StringKey(value = "StreamVerticle")
    fun provideStreamVerticle(verticle: StreamVerticle): Verticle {
        return verticle
    }
}