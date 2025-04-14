package com.tm.kotlin.service.counter

fun main() {
    val component = DaggerServiceComponent.create()
    component.getDeploymentService().start()
}