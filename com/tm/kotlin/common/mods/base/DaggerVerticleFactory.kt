package com.tm.kotlin.common.mods.base

import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.spi.VerticleFactory
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Provider

class DaggerVerticleFactory @Inject constructor(
    private val verticleMap: Map<String, @JvmSuppressWildcards Provider<Verticle>>
) : VerticleFactory {
    override fun prefix(): String? {
        return "dagger"
    }

    override fun createVerticle(
        verticleName: String,
        classLoader: ClassLoader,
        promise: Promise<Callable<Verticle?>>
    ) {
        val name = VerticleFactory.removePrefix(verticleName)

        val provider = verticleMap[name]
        if (provider != null) {
            promise.complete(Callable { provider.get() })
        } else {
            promise.fail(IllegalArgumentException("No verticle found for name: $name"))
        }
    }
}