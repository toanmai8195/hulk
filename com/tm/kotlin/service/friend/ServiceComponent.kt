package com.tm.kotlin.service.friend

import com.tm.kotlin.common.mods.base.DeploymentService
import com.tm.kotlin.common.mods.base.MBase
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        MBase::class,
        ServiceModule::class
    ]
)
interface ServiceComponent {
    fun getDeploymentService(): DeploymentService
}