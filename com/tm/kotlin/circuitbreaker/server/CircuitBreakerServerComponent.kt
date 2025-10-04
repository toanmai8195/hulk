package com.tm.kotlin.circuitbreaker.server

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CircuitBreakerServerModule::class])
interface CircuitBreakerServerComponent {
    fun getDeploymentService(): DeploymentService
}
