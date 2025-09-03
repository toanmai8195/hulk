package com.tm.kotlin.common.http.handler

import io.vertx.core.json.JsonObject

interface IApiHandler {
    suspend fun handle(requestBody: JsonObject): JsonObject
}