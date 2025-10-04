package com.tm.kotlin.circuitbreaker.server

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestHandler @Inject constructor() {

    companion object {
        val currentDelay = AtomicLong(1000L) // Default delay: 1s
        val shouldError = java.util.concurrent.atomic.AtomicBoolean(false) // Error flag
    }

    suspend fun handle(cmdId: Int): String {
        val delayMs = currentDelay.get()
        println("[$cmdId] Main request started (delay=${delayMs}ms, shouldError=${shouldError.get()})")
        delay(delayMs)

        if (shouldError.get()) {
            println("[$cmdId] Main request returning error 503")
            throw Exception("Service Unavailable")
        }

        println("[$cmdId] Main request completed")
        return """{"status":"success","cmdId":$cmdId,"delay":$delayMs,"message":"Request completed"}"""
    }
}
