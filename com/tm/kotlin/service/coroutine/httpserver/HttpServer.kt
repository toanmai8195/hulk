package com.tm.kotlin.service.coroutine.httpserver

fun main() {
    val component = DaggerHttpServerComponent.create()
    component.getDeploymentService().start()
}