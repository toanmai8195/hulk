package com.tm.kotlin.friendrecommend

fun main() {
    val component = DaggerFriendRecommendComponent.create()
    component.getDeploymentService().start()
}