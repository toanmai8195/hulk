package com.tm.kotlin.friendrecommend

import com.tm.kotlin.common.error.HulkException
import com.tm.kotlin.common.http.tracking.Tracking
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

class FriendRecommendHttpVerticle @Inject constructor(
    private val tracking: Tracking
) : CoroutineVerticle() {

    override suspend fun start() {
        val router = Router.router(vertx)

        // GET /hello - Simple hello world endpoint
        router.get("/hello").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val result = tracking.track("/hello") {
                        JsonObject().put("message", "Hello World from Friend Recommend Service!")
                    }
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.encode())
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // GET /health - Health check endpoint
        router.get("/health").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val result = tracking.track("/health") {
                        JsonObject()
                            .put("status", "healthy")
                            .put("service", "friend-recommend")
                            .put("timestamp", System.currentTimeMillis())
                    }
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.encode())
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(1996)
            .onSuccess {
                println("🚀 Friend Recommend HTTP server started on port 1996")
            }
            .onFailure {
                println("❌ Failed to start Friend Recommend HTTP server: ${it.message}")
            }
    }

    private fun handleError(routingContext: RoutingContext, ex: Exception) {
        val responseBody = (ex as? HulkException ?: HulkException(ex)).toResponse()
        routingContext.response()
            .putHeader("Content-Type", "application/json")
            .end(responseBody.encode())
    }

    override suspend fun stop() {
        println("FriendRecommendHttpVerticle shutting down gracefully...")
    }
}