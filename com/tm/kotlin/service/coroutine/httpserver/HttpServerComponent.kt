package com.tm.kotlin.service.coroutine.httpserver

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        HttpServerModule::class,
    ]
)
interface HttpServerComponent {
    fun getDeploymentService(): DeploymentService
}