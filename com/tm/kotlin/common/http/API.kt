package com.tm.kotlin.common.http

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

data class API(
    val httpMethod: HttpMethod,
    val path: String,
    val handler: Handler<RoutingContext>
)