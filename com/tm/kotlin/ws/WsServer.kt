package com.tm.kotlin.ws

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router

class WsServer {
}

fun main() {
    val vertx = Vertx.vertx()
    val server = vertx.createHttpServer(HttpServerOptions().setPort(8080))
    val router = Router.router(vertx)

    server.webSocketHandler { webSocket ->
        if (webSocket.path() != "/ws/chat") {
            println("Rejected connection from ${webSocket.remoteAddress()} on path ${webSocket.path()}")
            webSocket.close((1008).toShort(), "Invalid path")
            return@webSocketHandler
        }

        println("Client connected: ${webSocket.remoteAddress()}")

        webSocket.textMessageHandler { message ->
            println("Received: $message")
            webSocket.writeTextMessage("Echo: $message")
        }

        webSocket.exceptionHandler {
            println("WebSocket error: ${it.message}")
        }

        webSocket.closeHandler {
            println("Client disconnected")
        }
    }

    server.requestHandler(router).listen {
        if (it.succeeded()) {
            println("WebSocket server started on port 8080")
        } else {
            println("Failed to start server: ${it.cause().message}")
        }
    }
}