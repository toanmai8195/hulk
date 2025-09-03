package com.tm.kotlin.service.openapi

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        OpenApiModule::class
    ]
)
interface OpenApiComponent {
    fun getDeploymentService(): DeploymentService
}