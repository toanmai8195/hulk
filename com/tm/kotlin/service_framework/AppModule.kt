package com.tm.kotlin.service_framework

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.service_framework.mods.MHttp
import dagger.Module
import dagger.Provides

@Module(
    includes = [
        MBase::class,
        MMonitor::class,
        MHttp::class
    ]
)
class AppModule {
    @Provides
    fun provideServiceName(): String = "app_service"
}