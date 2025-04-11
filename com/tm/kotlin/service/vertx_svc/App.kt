package com.tm.kotlin.service.vertx_svc

fun main() {
    val component = DaggerAppComponent.create()
    component.getDeploymentService().start()
}