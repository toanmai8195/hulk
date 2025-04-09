package com.tm.kotlin.service_framework

fun main() {
    val component = DaggerAppComponent.create()
    component.getDeploymentService().start()
}