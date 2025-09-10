package com.tm.kotlin.service.counter.verticles.api

import com.tm.kotlin.service.counter.handler.ChatCounterHandler
import com.tm.kotlin.service.counter.handler.StatefulCounterHandler
import com.tm.kotlin.service.counter.handler.StatelessCounterHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Counter HTTP API Verticle - V2 with Handler Pattern
 * Clean separation: Verticle handles HTTP/routing, Handlers contain business logic
 */
class VCounterHttpApiV2 @Inject constructor(
    private val statelessHandler: StatelessCounterHandler,
    private val statefulHandler: StatefulCounterHandler<String>,
    private val chatHandler: ChatCounterHandler
) : AbstractVerticle() {

    override fun start(startPromise: Promise<Void>) {
        try {
            val router = io.vertx.ext.web.Router.router(vertx)
            router.route().handler(io.vertx.ext.web.handler.BodyHandler.create())
            
            setupStatelessRoutes(router)
            setupStatefulRoutes(router)
            setupChatRoutes(router)
            setupUtilityRoutes(router)
            
            vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080) { res ->
                    if (res.succeeded()) {
                        println("✅ Counter HTTP API V2 listening on port 8080")
                        startPromise.complete()
                    } else {
                        println("❌ Failed to start Counter HTTP API V2: ${res.cause()?.message}")
                        startPromise.fail(res.cause())
                    }
                }
        } catch (e: Exception) {
            println("❌ Error starting Counter HTTP API V2: ${e.message}")
            startPromise.fail(e)
        }
    }

    private fun setupStatelessRoutes(router: io.vertx.ext.web.Router) {
        // POST /api/v2/counter/stateless/{key}/increment
        router.post("/api/v2/counter/stateless/:key/increment").handler { ctx ->
            val key = ctx.pathParam("key")
            val delta = ctx.body().asJsonObject()?.getLong("delta", 1L) ?: 1L
            
            GlobalScope.launch {
                try {
                    val newTotal = statelessHandler.increment(key, delta)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("total", newTotal)
                        .put("delta", delta))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // GET /api/v2/counter/stateless/{key}
        router.get("/api/v2/counter/stateless/:key").handler { ctx ->
            val key = ctx.pathParam("key")
            
            GlobalScope.launch {
                try {
                    val total = statelessHandler.getTotal(key)
                    val lastUpdated = statelessHandler.getLastUpdated(key)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("total", total)
                        .put("lastUpdated", lastUpdated))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // POST /api/v2/counter/stateless/{key}/reset
        router.post("/api/v2/counter/stateless/:key/reset").handler { ctx ->
            val key = ctx.pathParam("key")
            
            GlobalScope.launch {
                try {
                    statelessHandler.reset(key)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("total", 0L)
                        .put("operation", "reset"))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // POST /api/v2/counter/stateless/batch
        router.post("/api/v2/counter/stateless/batch").handler { ctx ->
            val operations = ctx.body().asJsonObject()?.getJsonObject("operations")
            if (operations == null) {
                ctx.badRequest("Missing operations object")
                return@handler
            }
            
            GlobalScope.launch {
                try {
                    val operationsMap = operations.map.mapValues { (_, value) -> (value as Number).toLong() }
                    val results = statelessHandler.batchIncrement(operationsMap)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("results", JsonObject(results.mapValues { it.value })))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }
    }

    private fun setupStatefulRoutes(router: io.vertx.ext.web.Router) {
        // POST /api/v2/counter/stateful/{key}/increment
        router.post("/api/v2/counter/stateful/:key/increment").handler { ctx ->
            val key = ctx.pathParam("key")
            val body = ctx.body().asJsonObject() ?: JsonObject()
            val item = body.getString("item")
            val delta = body.getLong("delta", 1L)
            
            GlobalScope.launch {
                try {
                    val newTotal = statefulHandler.increment(key, item, delta)
                    val itemCount = item?.let { statefulHandler.getItemCount(key, it) }
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("item", item)
                        .put("total", newTotal)
                        .put("itemCount", itemCount)
                        .put("delta", delta))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // GET /api/v2/counter/stateful/{key}
        router.get("/api/v2/counter/stateful/:key").handler { ctx ->
            val key = ctx.pathParam("key")
            
            GlobalScope.launch {
                try {
                    val result = statefulHandler.getDetailedCounts(key)
                    ctx.jsonResponse(JsonObject()
                        .put("success", result.success)
                        .put("key", key)
                        .put("total", result.newTotal)
                        .put("items", JsonObject(result.allItems?.mapValues { it.value } ?: emptyMap()))
                        .put("itemCount", result.allItems?.size ?: 0)
                        .put("error", result.error))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // GET /api/v2/counter/stateful/{key}/item/{item}
        router.get("/api/v2/counter/stateful/:key/item/:item").handler { ctx ->
            val key = ctx.pathParam("key")
            val item = ctx.pathParam("item")
            
            GlobalScope.launch {
                try {
                    val count = statefulHandler.getItemCount(key, item)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("item", item)
                        .put("count", count))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // DELETE /api/v2/counter/stateful/{key}/item/{item}
        router.delete("/api/v2/counter/stateful/:key/item/:item").handler { ctx ->
            val key = ctx.pathParam("key")
            val item = ctx.pathParam("item")
            
            GlobalScope.launch {
                try {
                    statefulHandler.removeItem(key, item)
                    val result = statefulHandler.getDetailedCounts(key)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("key", key)
                        .put("removedItem", item)
                        .put("total", result.newTotal)
                        .put("items", JsonObject(result.allItems?.mapValues { it.value } ?: emptyMap())))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }
    }

    private fun setupChatRoutes(router: io.vertx.ext.web.Router) {
        // GET /api/v2/chat/unread/{userId}
        router.get("/api/v2/chat/unread/:userId").handler { ctx ->
            val userId = ctx.pathParam("userId")
            
            GlobalScope.launch {
                try {
                    val status = chatHandler.getUnreadStatus(userId)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("userId", status.userId)
                        .put("unreadRooms", status.unreadRooms)
                        .put("unreadMessagesPerRoom", JsonObject(status.unreadMessagesPerRoom.mapValues { it.value }))
                        .put("totalUnreadMessages", status.unreadMessagesPerRoom.values.sum()))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // POST /api/v2/chat/mark-read/{userId}/{roomId}
        router.post("/api/v2/chat/mark-read/:userId/:roomId").handler { ctx ->
            val userId = ctx.pathParam("userId")
            val roomId = ctx.pathParam("roomId")
            
            GlobalScope.launch {
                try {
                    val result = chatHandler.markRoomAsRead(userId, roomId)
                    ctx.jsonResponse(JsonObject()
                        .put("success", result.success)
                        .put("userId", userId)
                        .put("roomId", roomId)
                        .put("unreadRooms", result.newTotal)
                        .put("remainingMessages", JsonObject(result.allItems?.mapValues { it.value } ?: emptyMap()))
                        .put("error", result.error))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        // GET /api/v2/chat/room/{roomId}/total-unread
        router.get("/api/v2/chat/room/:roomId/total-unread").handler { ctx ->
            val roomId = ctx.pathParam("roomId")
            
            GlobalScope.launch {
                try {
                    val count = chatHandler.getTotalRoomUnreadCount(roomId)
                    ctx.jsonResponse(JsonObject()
                        .put("success", true)
                        .put("roomId", roomId)
                        .put("totalUnreadCount", count))
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }
    }

    private fun setupUtilityRoutes(router: io.vertx.ext.web.Router) {
        // GET /health
        router.get("/health").handler { ctx ->
            ctx.jsonResponse(JsonObject()
                .put("status", "healthy")
                .put("service", "counter-api-v2")
                .put("version", "2.1.0")
                .put("architecture", "DAO → Handler → Verticle"))
        }

        // GET /api/v2/info
        router.get("/api/v2/info").handler { ctx ->
            ctx.jsonResponse(JsonObject()
                .put("service", "Generic Counter Service V2")
                .put("version", "2.1.0")
                .put("architecture", JsonObject()
                    .put("dao", "Database access layer")
                    .put("handler", "Business logic layer") 
                    .put("verticle", "HTTP/Kafka infrastructure layer"))
                .put("layers", JsonObject()
                    .put("StatelessCounterHandler", "Simple counter operations")
                    .put("StatefulCounterHandler", "Counter with item tracking")
                    .put("ChatCounterHandler", "Chat-specific business logic"))
                .put("endpoints", JsonObject()
                    .put("stateless", "/api/v2/counter/stateless/*")
                    .put("stateful", "/api/v2/counter/stateful/*")
                    .put("chat", "/api/v2/chat/*")))
        }
    }

    // Extension function for cleaner JSON responses
    private fun io.vertx.ext.web.RoutingContext.jsonResponse(json: JsonObject) {
        response().putHeader("content-type", "application/json").end(json.toString())
    }

    private fun io.vertx.ext.web.RoutingContext.badRequest(message: String) {
        response().setStatusCode(400).putHeader("content-type", "application/json")
            .end(JsonObject().put("success", false).put("error", message).toString())
    }

    private fun handleError(ctx: io.vertx.ext.web.RoutingContext, error: Throwable) {
        ctx.response().setStatusCode(500).putHeader("content-type", "application/json")
            .end(JsonObject()
                .put("success", false)
                .put("error", error.message ?: "Unknown error")
                .put("timestamp", System.currentTimeMillis())
                .toString())
    }
}