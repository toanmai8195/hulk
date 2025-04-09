package com.tm.kotlin.common.mods.base

import io.vertx.core.*
import javax.inject.Inject
import javax.inject.Provider

class DeploymentService @Inject constructor(
    private val vertx: Vertx,
    private val options: Map<String, @JvmSuppressWildcards DeploymentOptions>,
    private val verticles: Map<String, @JvmSuppressWildcards Provider<Verticle>>,
    private val verticleFactory: DaggerVerticleFactory
) {
    companion object {
        private var isDeployed = false
    }

    fun start() {
        println("[Deploy] Deployment starting ...")
        vertx.registerVerticleFactory(verticleFactory)
        val futures = verticles.map { entry ->
            println("[Deploy] Deploying ${entry.key} verticle...")
            vertx.deployVerticle("dagger:${entry.key}", options[entry.key] ?: DeploymentOptions())
        }

        Future.all(
            futures
        ).onComplete { rs ->
            isDeployed = true
            if (rs!!.succeeded()) {
                println("[Deploy] Readied!")
            } else {
                println("[Deploy] Failed! {" + rs.cause().message + "}")
                rs.cause().printStackTrace()
            }
        }
    }
}