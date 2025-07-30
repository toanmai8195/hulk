package com.tm.kotlin.service.coroutine.httpserver.verticles

import com.tm.kotlin.common.error.ErrorCode
import com.tm.kotlin.common.error.HulkException
import com.tm.kotlin.service.coroutine.httpserver.handler.FriendByCellGenerate
import com.tm.kotlin.service.coroutine.httpserver.handler.FriendGenerate
import com.tm.kotlin.service.coroutine.httpserver.handler.IApiHandler
import com.tm.kotlin.service.coroutine.httpserver.handler.MutualFriendByCellCheck
import com.tm.kotlin.service.coroutine.httpserver.handler.MutualFriendCheck
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

class HttpServerVerticle @Inject constructor(
    private val mutualFriendCheck: MutualFriendCheck,
    private val mutualFriendByCellCheck: MutualFriendByCellCheck,
    private val friendGenerate: FriendGenerate,
    private val friendByCellGenerate: FriendByCellGenerate,
    private val tracking: Tracking,
    private val router: Router
) : CoroutineVerticle() {

    override suspend fun start() {
        router.route().handler(BodyHandler.create())

        initApi("/mutual-check", mutualFriendCheck)
        initApi("/generate", friendGenerate)
        initApi("/generate/by-cell", friendByCellGenerate)
        initApi("/mutual-check/by-cell", mutualFriendByCellCheck)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(1995)
            .onSuccess {
                println("🚀 HTTP server started on port 1995")
            }
            .onFailure {
                println("❌ Failed to start HTTP server: ${it.message}")
            }
    }

    private fun initApi(path: String, handler: IApiHandler, method: HttpMethod = HttpMethod.POST) {
        when (method) {
            HttpMethod.GET -> router.get(path)
            HttpMethod.POST -> router.post(path)
            else -> throw HulkException(ErrorCode.REQUEST_ERROR, "Method not found!")
        }.handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val body = ctx.body().asJsonObject()
                    val result = tracking.track(path) {
                        handler.handle(body)
                    }
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.encode())
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }
    }

    private fun handleError(routingContext: RoutingContext, ex: Exception) {
        val responseBody = (ex as? HulkException ?: HulkException(ex)).toResponse()
        routingContext.response()
            .putHeader("Content-Type", "application/json")
            .end(responseBody.encode())
    }

    override suspend fun stop() {
        println("HttpServerVerticle shutting down gracefully...")
    }
}