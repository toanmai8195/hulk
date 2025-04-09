package com.tm.kotlin.common.mods.base

import dagger.Module
import dagger.multibindings.Multibinds
import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle

@Module
abstract class MBaseBinding {
    @Multibinds
    abstract fun provideEmptyVerticles(): Map<String, Verticle>

    @Multibinds
    abstract fun provideEmptyVerticlesOption(): Map<String, DeploymentOptions>
}