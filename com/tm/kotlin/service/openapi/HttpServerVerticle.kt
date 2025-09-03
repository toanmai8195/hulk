package com.tm.kotlin.service.openapi

import com.tm.kotlin.common.error.HulkException
import com.tm.kotlin.common.http.tracking.Tracking
import com.tm.kotlin.service.openapi.handlers.TestGetHandlers
import com.tm.kotlin.service.openapi.handlers.TestPostHandlers
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

class HttpServerVerticle @Inject constructor(
    private val tracking: Tracking,
    private val testGetHandlers: TestGetHandlers,
    private val testPostHandlers: TestPostHandlers,
) : CoroutineVerticle() {

    override suspend fun start() {
        RouterBuilder.create(vertx, "com/tm/kotlin/service/openapi/openapi.yaml").onSuccess { factory ->
            // POST /test/post
            factory.operation("postTest").handler { ctx ->
                launch(ctx.vertx().dispatcher()) {
                    try {
                        val body = ctx.body().asJsonObject()
                        val result = tracking.track("/test/post") {
                            testPostHandlers.handle(body)
                        }
                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(result.encode())
                    } catch (e: Exception) {
                        handleError(ctx, e)
                    }
                }
            }

            // GET /test/get
            factory.operation("getTest").handler { ctx ->
                launch(ctx.vertx().dispatcher()) {
                    try {
                        val params = JsonObject()
                        ctx.queryParams().forEach { entry ->
                            params.put(
                                entry.key,
                                entry.value
                            )
                        }
                        val result = tracking.track("/test/get") {
                            testGetHandlers.handle(params)
                        }
                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(result.encode())
                    } catch (e: Exception) {
                        handleError(ctx, e)
                    }
                }
            }

            val router = factory.createRouter()

            router.get("/openapi.yaml").handler { ctx ->
                vertx.fileSystem().readFile("com/tm/kotlin/service/openapi/openapi.yaml") { ar ->
                    if (ar.succeeded()) {
                        ctx.response()
                            .putHeader("Content-Type", "application/yaml")
                            .end(ar.result().toString())
                    } else {
                        ctx.response().setStatusCode(404).end("openapi.yaml not found in filesystem")
                    }
                }
            }

            // Serve Swagger UI
            router.get("/docs").handler { ctx ->
                ctx.response()
                    .putHeader("content-type", "text/html")
                    .end(
                        """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <title>Swagger UI</title>
                      <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist/swagger-ui.css" >
                      <script src="https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js"></script>
                      <script src="https://unpkg.com/swagger-ui-dist/swagger-ui-standalone-preset.js"></script>
                    </head>
                    <body>
                    <div id="swagger-ui"></div>
                    <script>
                      const ui = SwaggerUIBundle({
                        url: '/openapi.yaml',
                        dom_id: '#swagger-ui',
                        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                        layout: "StandaloneLayout"
                      })
                    </script>
                    </body>
                    </html>
                    """.trimIndent()
                    )
            }

            vertx.createHttpServer()
                .requestHandler(router)
                .listen(1995)
                .onSuccess {
                    println("🚀 HTTP server started on port 1995")
                }
                .onFailure {
                    println("❌ Failed to start HTTP server: ${it.message}")
                }
        }.onFailure {
            println("❌ Failed to create RouterBuilder: ${it.message}")
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