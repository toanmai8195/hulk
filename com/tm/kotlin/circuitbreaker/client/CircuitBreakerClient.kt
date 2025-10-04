package com.tm.kotlin.circuitbreaker.client

fun main() {
    val component = DaggerCircuitBreakerClientComponent.create()
    component.getDeploymentService().start()
}
