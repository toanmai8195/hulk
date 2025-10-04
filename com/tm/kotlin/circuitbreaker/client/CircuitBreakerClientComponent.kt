package com.tm.kotlin.circuitbreaker.client

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CircuitBreakerClientModule::class])
interface CircuitBreakerClientComponent {
    fun getDeploymentService(): DeploymentService
}
