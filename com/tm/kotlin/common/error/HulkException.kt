package com.tm.kotlin.common.error

import io.vertx.core.json.JsonObject

class HulkException(
    val errorCode: Int,
    val errorMessage: String,
    throwable: Throwable? = null
) : Exception(throwable) {

    constructor(throwable: Throwable) : this(
        ErrorCode.UNEXPECTED_ERROR,
        throwable.message ?: throwable.cause?.message ?: "Unexpected error",
        throwable
    )

    fun toResponse(): JsonObject {
        return JsonObject()
            .put("error_code", errorCode)
            .put("error_message", errorMessage)
    }
}