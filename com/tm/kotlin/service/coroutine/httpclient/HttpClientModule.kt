package com.tm.kotlin.service.coroutine.httpclient

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.service.coroutine.httpclient.verticles.HttpClientVerticle
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module(
    includes = [
        MBase::class
    ]
)
class HttpClientModule {
    @Provides
    @IntoMap
    @StringKey(value = "HttpClientVerticle")
    fun provideHttpVerticle(verticle: HttpClientVerticle): Verticle {
        return verticle
    }
}