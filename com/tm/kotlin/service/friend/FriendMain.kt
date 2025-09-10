package com.tm.kotlin.service.friend

import java.util.concurrent.Executors

class CounterMain {
    companion object {
        const val RPS = 20
        const val BITMAP_ACTOR = "11000000000"
        const val BITMAP_PARTNER = "11000000001"
        const val BITMAP_ACTOR_10K = "11000000002"
        const val BITMAP_PARTNER_10K = "11000000003"
        const val CELL_ACTOR = "11000000004"
        const val CELL_ACTOR_10K = "11000000005"
        const val CELL_PARTNER = "11000000006"
        const val CELL_PARTNER_10K = "11000000007"
        val executor = Executors.newCachedThreadPool()
    }
}

fun main() {
    val component = DaggerServiceComponent.create()
    component.getDeploymentService().start()
}