package com.tm.kotlin.service.bitmap_loadtest

import com.tm.kotlin.common.mods.base.DeploymentService
import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        MBase::class,
        MMonitor::class,
        ServiceModule::class
    ]
)
interface ServiceComponent {
    fun getDeploymentService(): DeploymentService
}