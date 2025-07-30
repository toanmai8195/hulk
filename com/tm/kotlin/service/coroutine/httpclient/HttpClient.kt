package com.tm.kotlin.service.coroutine.httpclient

fun main() {
    val component = DaggerHttpClientComponent.create()
    component.getDeploymentService().start()
}