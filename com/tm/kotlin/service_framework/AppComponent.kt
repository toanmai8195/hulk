package com.tm.kotlin.service_framework

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun getDeploymentService(): DeploymentService
}