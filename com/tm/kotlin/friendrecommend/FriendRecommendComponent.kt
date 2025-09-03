package com.tm.kotlin.friendrecommend

import com.tm.kotlin.common.mods.base.DeploymentService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FriendRecommendModule::class
    ]
)
interface FriendRecommendComponent {
    fun getDeploymentService(): DeploymentService
}