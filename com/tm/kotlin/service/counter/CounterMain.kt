package com.tm.kotlin.service.counter

fun main() {
    println("🚀 Starting Counter Service...")
    val component = DaggerServiceComponent.create()
    component.getDeploymentService().start()
    println("✅ Counter Service running with Clean Architecture!")
}