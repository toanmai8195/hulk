package com.tm.kotlin.backupdata

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class CircuitBreakerState {
    CLOSED, OPEN, HALF_OPEN
}

@Singleton
class CircuitBreaker @Inject constructor() {
    private var state = CircuitBreakerState.CLOSED
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    
    private val failureThreshold = 5
    private val timeout = 60000L // 1 minute
    private val halfOpenMaxCalls = 3
    private var halfOpenCalls = AtomicInteger(0)

    suspend fun <T> execute(operation: suspend () -> T): T {
        when (state) {
            CircuitBreakerState.CLOSED -> {
                return try {
                    val result = operation()
                    onSuccess()
                    result
                } catch (e: Exception) {
                    onFailure()
                    throw e
                }
            }
            CircuitBreakerState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime.get() > timeout) {
                    state = CircuitBreakerState.HALF_OPEN
                    halfOpenCalls.set(0)
                    return execute(operation)
                } else {
                    throw RuntimeException("Circuit breaker is OPEN")
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                if (halfOpenCalls.getAndIncrement() < halfOpenMaxCalls) {
                    return try {
                        val result = operation()
                        onSuccess()
                        result
                    } catch (e: Exception) {
                        onFailure()
                        throw e
                    }
                } else {
                    throw RuntimeException("Circuit breaker HALF_OPEN limit exceeded")
                }
            }
        }
    }

    private fun onSuccess() {
        failureCount.set(0)
        state = CircuitBreakerState.CLOSED
    }

    private fun onFailure() {
        val failures = failureCount.incrementAndGet()
        lastFailureTime.set(System.currentTimeMillis())
        
        if (failures >= failureThreshold) {
            state = CircuitBreakerState.OPEN
        }
    }

    fun getState(): CircuitBreakerState = state
}