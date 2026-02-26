package com.tm.kotlin.hbase_load_test

fun main() {
    val component = DaggerServiceComponent.create()
    component.getDeploymentService().start()
}
