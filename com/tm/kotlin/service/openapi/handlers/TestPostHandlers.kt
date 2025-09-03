package com.tm.kotlin.service.openapi.handlers

import com.tm.kotlin.common.http.handler.IApiHandler
import io.vertx.core.json.JsonObject
import javax.inject.Inject

class TestPostHandlers @Inject constructor(
) : IApiHandler {
    override suspend fun handle(requestBody: JsonObject): JsonObject {
        val name = requestBody.getString("name") ?: "UnKnown"

        return JsonObject()
            .put("greeting", "Hello $name! I'm post")
    }
}