package com.tm.kotlin.hbase_load_test

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
