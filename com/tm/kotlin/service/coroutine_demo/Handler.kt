package com.tm.kotlin.service.coroutine_demo

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class Handler {

    suspend fun blockingHandlerWithCoroutine(context: RoutingContext) {
        println("blockingHandlerWithCoroutine -IN- Thread=${Thread.currentThread().name}")

        try {
            val rs: String
            withContext(Dispatchers.IO) {
                println("blockingHandlerWithCoroutine -HANDLE- Thread=${Thread.currentThread().name}")
                rs = executeHeavyTask()
            }
            context.response()
                .putHeader("content-type", "application/json")
                .end(rs)
        } catch (e: Exception) {
            context.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end("{\"error\": \"${e.message}\"}")
        }
    }

    fun blockingHandlerWithoutCoroutine(context: RoutingContext) {
        println("blockingHandlerWithoutCoroutine - Thread=${Thread.currentThread().name}")
        try {
            val rs = executeHeavyTask()
            context.response()
                .putHeader("content-type", "application/json")
                .end(rs)
        } catch (e: Exception) {
            context.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end("{\"error\": \"${e.message}\"}")
        }
    }

    private fun executeHeavyTask(): String {
        if (Random.nextBoolean()){
            throw Exception("abc")
        }
        Thread.sleep(5000)
        return "OK"
    }
}