package com.tm.kotlin.circuitbreaker.server

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

class CircuitBreakerServerVerticle @Inject constructor(
    private val router: Router,
    private val testHandler: TestHandler
) : CoroutineVerticle() {

    private val cmdIdCounter = java.util.concurrent.atomic.AtomicInteger(0)

    override suspend fun start() {
        router.route().handler(BodyHandler.create())

        // API 1: Test endpoint with circuit breaker
        router.get("/test").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val cmdId = cmdIdCounter.incrementAndGet()
                    val result = testHandler.handle(cmdId)
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result)
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // API 2: Backup test endpoint - always fast (1s delay)
        router.get("/backup/test").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val cmdId = cmdIdCounter.incrementAndGet()
                    println("[$cmdId] Backup request started")
                    kotlinx.coroutines.delay(1000) // Fixed 1s delay
                    println("[$cmdId] Backup request completed")
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end("""{"status":"success","cmdId":$cmdId,"delay":1000,"message":"Backup endpoint - fast response","backup":true}""")
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // API 3: Configure delay endpoint
        router.post("/config/delay").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val body = ctx.body().asJsonObject()
                    val newDelay = body.getLong("delay")

                    if (newDelay < 0) {
                        ctx.response()
                            .setStatusCode(400)
                            .putHeader("Content-Type", "application/json")
                            .end("""{"status":"error","message":"Delay must be non-negative"}""")
                        return@launch
                    }

                    TestHandler.currentDelay.set(newDelay)

                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end("""{"status":"success","delay":$newDelay,"message":"Delay updated"}""")
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // API 4: Configure error endpoint
        router.post("/config/error").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val body = ctx.body().asJsonObject()
                    val shouldError = body.getBoolean("error")

                    TestHandler.shouldError.set(shouldError)

                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end("""{"status":"success","error":$shouldError,"message":"Error flag updated"}""")
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onSuccess {
                println("🚀 Circuit Breaker Server started on port 8080")
                println("   - GET  /test - Test endpoint with configurable delay")
                println("   - GET  /backup/test - Backup endpoint (always 1s delay)")
                println("   - POST /config/delay - Configure delay (body: {\"delay\": <milliseconds>})")
                println("   - POST /config/error - Configure error mode (body: {\"error\": true/false})")
            }
            .onFailure {
                println("❌ Failed to start server: ${it.message}")
            }
    }

    private fun handleError(ctx: RoutingContext, ex: Exception) {
        val statusCode = if (ex.message?.contains("Service Unavailable") == true) 503 else 500
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end("""{"status":"error","message":"${ex.message}"}""")
    }

    override suspend fun stop() {
        println("CircuitBreakerServerVerticle shutting down...")
    }
}
