package com.tm.kotlin.circuitbreaker.server

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestHandler @Inject constructor() {

    companion object {
        val currentDelay = AtomicLong(1000L) // Default delay: 1s
    }

    suspend fun handle(): String {
        val delayMs = currentDelay.get()
        delay(delayMs)
        return """{"status":"success","delay":$delayMs,"message":"Request completed"}"""
    }
}
