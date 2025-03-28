package com.tm.consumer

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    val workerOption = DeploymentOptions()
    workerOption.threadingModel = ThreadingModel.WORKER
    workerOption.instances = 1

    val consumerOption = DeploymentOptions()
    workerOption.threadingModel = ThreadingModel.WORKER
    workerOption.instances = 1

    Future.all(
        listOf(
            vertx.deployVerticle(MonitoringVerticle()),
            vertx.deployVerticle(
                ConsumerVertical("high-priority-topic", "high-priority-consumer", "high"),
                consumerOption
            ),
            vertx.deployVerticle(
                ConsumerVertical("low-priority-topic", "low-priority-consumer", "low"),
                consumerOption
            ),
            vertx.deployVerticle(HighPriorityWorkerVertical::class.java, workerOption),
            vertx.deployVerticle(LowPriorityWorkerVertical::class.java, workerOption),
        )
    ).onComplete { rs ->
        if (rs.succeeded()) {
            println("Deployed!")
        } else {
            rs.cause().printStackTrace()
        }
    }
}