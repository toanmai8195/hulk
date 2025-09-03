package com.tm.kotlin.common.http.tracking

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Tracking @Inject constructor(
    private val registry: PrometheusMeterRegistry
) {
    private val counterMap = mutableMapOf<String, Counter>()
    private val timerMap = mutableMapOf<String, Timer>()

    private fun getOrCreateCounter(endpoint: String): Counter =
        counterMap.getOrPut(endpoint) {
            Counter.builder("http_api_request")
                .description("Count API calls")
                .tag("endpoint", endpoint)
                .register(registry)
        }

    private fun getOrCreateTimer(endpoint: String): Timer =
        timerMap.getOrPut(endpoint) {
            Timer.builder("http_api_latency")
                .description("Latency of API calls")
                .tag("endpoint", endpoint)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry)
        }

    suspend fun <T> track(endpoint: String, block: suspend () -> T): T {
        getOrCreateCounter(endpoint).increment()
        val sample = Timer.start(registry)
        val result = block()
        sample.stop(getOrCreateTimer(endpoint))
        return result
    }
}