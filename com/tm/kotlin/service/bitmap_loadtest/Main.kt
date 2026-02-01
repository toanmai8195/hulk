package com.tm.kotlin.service.bitmap_loadtest

fun main() {
    val component = DaggerServiceComponent.create()
    component.getDeploymentService().start()
}