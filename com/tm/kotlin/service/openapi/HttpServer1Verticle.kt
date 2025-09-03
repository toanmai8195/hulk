package com.tm.kotlin.service.openapi

import com.tm.kotlin.common.error.HulkException
import com.tm.kotlin.common.http.tracking.Tracking
import com.tm.kotlin.service.openapi.handlers.TestGetAnnotationHandlers
import com.tm.kotlin.service.openapi.handlers.TestPostAnnotationHandlers
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.OpenAPI
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@Tag(name = "Test API", description = "Các API demo get/post")
class HttpServer1Verticle @Inject constructor(
    private val tracking: Tracking,
    private val testGetAnnotationHandlers: TestGetAnnotationHandlers,
    private val testPostAnnotationHandlers: TestPostAnnotationHandlers
) : CoroutineVerticle() {

    override suspend fun start() {
        val router = Router.router(vertx)

        // POST /test/post
        router.post("/test/post").handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    val body = ctx.body().asJsonObject()
                    val result = tracking.track("/test/post") {
                        testPostAnnotationHandlers.handle(body)
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
        router.get("/test/get").handler { ctx ->
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
                        testGetAnnotationHandlers.handle(params)
                    }
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.encode())
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }

        router.get("/openapi.json").handler { ctx ->
            val openApi = generateOpenApi()
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(io.vertx.core.json.Json.encodePrettily(openApi))
        }

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
                    url: '/openapi.json',
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
            .listen(1997)
            .onSuccess {
                println("🚀 HTTP server started on port 1997")
                println("🚀 Api documents on http://localhost:1997/docs")

            }
            .onFailure {
                println("❌ Failed to start HTTP server: ${it.message}")
            }
    }


    private fun handleError(routingContext: RoutingContext, ex: Exception) {
        val responseBody = (ex as? HulkException ?: HulkException(ex)).toResponse()
        routingContext.response()
            .putHeader("Content-Type", "application/json")
            .end(responseBody.encode())
    }

    fun generateOpenApi(): OpenAPI {
        return OpenAPI().apply {
            info = io.swagger.v3.oas.models.info.Info()
                .title("Test API")
                .description("API for testing GET and POST operations")
                .version("1.0.0")
            
            servers = listOf(
                io.swagger.v3.oas.models.servers.Server()
                    .url("http://localhost:1997")
                    .description("Local development server")
            )
            
            paths = io.swagger.v3.oas.models.Paths().apply {
                // GET /test/get endpoint
                addPathItem("/test/get", io.swagger.v3.oas.models.PathItem().apply {
                    get = io.swagger.v3.oas.models.Operation().apply {
                        summary = "Get test data"
                        description = "Receives query parameters and returns processed result"
                        operationId = "getTest"
                        tags = listOf("Test API")
                        parameters = listOf(
                            io.swagger.v3.oas.models.parameters.Parameter().apply {
                                name = "name"
                                `in` = "query"
                                description = "Name parameter"
                                required = false
                                schema = io.swagger.v3.oas.models.media.Schema<String>().apply {
                                    type = "string"
                                }
                            }
                        )
                        responses = io.swagger.v3.oas.models.responses.ApiResponses().apply {
                            addApiResponse("200", io.swagger.v3.oas.models.responses.ApiResponse().apply {
                                description = "Successful response"
                                content = io.swagger.v3.oas.models.media.Content().apply {
                                    addMediaType("application/json", io.swagger.v3.oas.models.media.MediaType().apply {
                                        schema = io.swagger.v3.oas.models.media.Schema<Any>().apply {
                                            type = "object"
                                            properties = mapOf(
                                                "greeting" to io.swagger.v3.oas.models.media.Schema<String>().apply {
                                                    type = "string"
                                                }
                                            )
                                        }
                                    })
                                }
                            })
                        }
                    }
                })
                
                // POST /test/post endpoint  
                addPathItem("/test/post", io.swagger.v3.oas.models.PathItem().apply {
                    post = io.swagger.v3.oas.models.Operation().apply {
                        summary = "Post test data"
                        description = "Receives JSON body and returns processed result"
                        operationId = "postTest"
                        tags = listOf("Test API")
                        requestBody = io.swagger.v3.oas.models.parameters.RequestBody().apply {
                            description = "Test request data"
                            required = true
                            content = io.swagger.v3.oas.models.media.Content().apply {
                                addMediaType("application/json", io.swagger.v3.oas.models.media.MediaType().apply {
                                    schema = io.swagger.v3.oas.models.media.Schema<Any>().apply {
                                        type = "object"
                                        properties = mapOf(
                                            "name" to io.swagger.v3.oas.models.media.Schema<String>().apply {
                                                type = "string"
                                            }
                                        )
                                    }
                                })
                            }
                        }
                        responses = io.swagger.v3.oas.models.responses.ApiResponses().apply {
                            addApiResponse("200", io.swagger.v3.oas.models.responses.ApiResponse().apply {
                                description = "Successful response"
                                content = io.swagger.v3.oas.models.media.Content().apply {
                                    addMediaType("application/json", io.swagger.v3.oas.models.media.MediaType().apply {
                                        schema = io.swagger.v3.oas.models.media.Schema<Any>().apply {
                                            type = "object"
                                            properties = mapOf(
                                                "greeting" to io.swagger.v3.oas.models.media.Schema<String>().apply {
                                                    type = "string"
                                                }
                                            )
                                        }
                                    })
                                }
                            })
                        }
                    }
                })
            }
        }
    }

    override suspend fun stop() {
        println("HttpServerVerticle shutting down gracefully...")
    }
}