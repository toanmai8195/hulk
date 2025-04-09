package com.tm.kotlin.common.http

interface HttpFactory {
    fun create(serviceName: String, port: Int, apis: List<API>): VHttp
}