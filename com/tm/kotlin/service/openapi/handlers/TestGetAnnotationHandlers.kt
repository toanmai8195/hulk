package com.tm.kotlin.service.openapi.handlers

import com.tm.kotlin.common.http.handler.IApiHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.vertx.core.json.JsonObject
import javax.inject.Inject


class TestGetAnnotationHandlers @Inject constructor(
) : IApiHandler {

    @Operation(
        summary = "Get test data",
        description = "Nhận query params và trả kết quả xử lý",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Kết quả OK",
                content = [Content(schema = Schema(implementation = JsonObject::class))]
            )
        ]
    )
    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val name = requestBody.getString("name") ?: "UnKnown"

        return JsonObject()
            .put("greeting", "Hello $name! I'm get")
    }
}