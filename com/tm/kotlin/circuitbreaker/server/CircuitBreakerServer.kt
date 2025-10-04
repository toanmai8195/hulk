package com.tm.kotlin.circuitbreaker.server

fun main() {
    val component = DaggerCircuitBreakerServerComponent.create()
    component.getDeploymentService().start()
}
