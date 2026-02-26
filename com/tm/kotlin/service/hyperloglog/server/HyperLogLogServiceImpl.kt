package com.tm.kotlin.service.hyperloglog.server

import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class HyperLogLogServiceImpl(
    private val store: HyperLogLogStore,
    private val registry: PrometheusMeterRegistry
) : HyperLogLogServiceGrpc.HyperLogLogServiceImplBase() {

    private val logger = LoggerFactory.getLogger(HyperLogLogServiceImpl::class.java)

    private val segmentsAddedCounter = Counter.builder("hyperloglog_segments_added_total")
        .description("Total number of segments added")
        .register(registry)

    private val requestsCounter = Counter.builder("hyperloglog_requests_total")
        .description("Total number of AddSegments requests")
        .register(registry)

    private val requestTimer = Timer.builder("hyperloglog_request_duration_seconds")
        .description("Request processing duration")
        .publishPercentileHistogram(true)
        .register(registry)

    override fun addSegments(
        request: AddSegmentsRequest,
        responseObserver: StreamObserver<AddSegmentsResponse>
    ) {
        val startTime = System.nanoTime()

        try {
            val segments = request.segmentsList
            val (dailyCardinality, hourlyCardinality) = store.addSegments(segments)

            segmentsAddedCounter.increment(segments.size.toDouble())
            requestsCounter.increment()

            val response = AddSegmentsResponse.newBuilder()
                .setEstimatedCardinalityDaily(dailyCardinality)
                .setEstimatedCardinalityHourly(hourlyCardinality)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

            if (requestsCounter.count().toLong() % 1000 == 0L) {
                logger.info("Processed ${requestsCounter.count().toLong()} requests, " +
                    "daily cardinality: $dailyCardinality, hourly cardinality: $hourlyCardinality")
            }
        } catch (e: Exception) {
            logger.error("Error processing AddSegments request", e)
            responseObserver.onError(e)
        } finally {
            val duration = System.nanoTime() - startTime
            requestTimer.record(duration, TimeUnit.NANOSECONDS)
        }
    }

    override fun getCardinality(
        request: GetCardinalityRequest,
        responseObserver: StreamObserver<GetCardinalityResponse>
    ) {
        try {
            val timeBucket = request.timeBucket
            val cardinality = store.getCardinality(timeBucket)

            val response = GetCardinalityResponse.newBuilder()
                .setTimeBucket(timeBucket)
                .setEstimatedCardinality(cardinality)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Error processing GetCardinality request", e)
            responseObserver.onError(e)
        }
    }
}
