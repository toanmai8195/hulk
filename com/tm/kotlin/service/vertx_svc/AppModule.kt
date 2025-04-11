package com.tm.kotlin.service.vertx_svc

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
import com.tm.kotlin.service.vertx_svc.mods.MHttp
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