package com.tm.kotlin.service.coroutine.httpserver.handler

import io.vertx.core.json.JsonObject

interface IApiHandler {
    suspend fun handle(requestBody: JsonObject): JsonObject
}