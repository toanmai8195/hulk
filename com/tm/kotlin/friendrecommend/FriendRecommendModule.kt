package com.tm.kotlin.friendrecommend

import com.tm.kotlin.common.mods.base.MBase
import com.tm.kotlin.common.mods.monitor.MMonitor
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.vertx.core.Verticle

@Module(
    includes = [
        MBase::class,
        MMonitor::class
    ]
)
class FriendRecommendModule {
    @Provides
    @IntoMap
    @StringKey(value = "FriendRecommendHttpVerticle")
    fun provideHttpVerticle(verticle: FriendRecommendHttpVerticle): Verticle {
        return verticle
    }
}