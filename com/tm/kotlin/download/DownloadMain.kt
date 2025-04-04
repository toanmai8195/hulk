package com.tm.kotlin.download

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    val vertical = DownloadVerticle()
    Future.all(
        listOf(
            vertx.deployVerticle(
                vertical,
                DeploymentOptions()
                    .setThreadingModel(ThreadingModel.WORKER)
            )
        )
    ).onComplete {
        if (it.succeeded()) {
            println("Deployed!")
        } else {
            println("Deploy failed!")
        }
    }
}