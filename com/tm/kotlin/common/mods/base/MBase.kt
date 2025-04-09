package com.tm.kotlin.common.mods.base

import dagger.Module
import dagger.Provides
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import javax.inject.Singleton

@Module(
    includes = [
        MBaseBinding::class
    ]
)
class MBase {
    @Provides
    @Singleton
    fun provideVertx(): Vertx = Vertx.vertx()

    @Provides
    @Singleton
    fun provideRoute(vertx: Vertx): Router = Router.router(vertx)
}