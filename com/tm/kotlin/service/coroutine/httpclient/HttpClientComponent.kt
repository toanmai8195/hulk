package com.tm.kotlin.service.coroutine.httpclient

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        HttpClientModule::class,
    ]
)
interface HttpClientComponent {
    fun getDeploymentService(): DeploymentService
}