package com.tm.kotlin.service.openapi

fun main() {
    val component = DaggerOpenApiComponent.create()
    component.getDeploymentService().start()
}