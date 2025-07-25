package com.tm.kotlin.service.coroutine.grpc_server

import com.tm.kotlin.models.grpc.HelloServiceGrpc
import com.tm.kotlin.models.grpc.HelloRequest
import com.tm.kotlin.models.grpc.HelloResponse
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrpcServer : HelloServiceGrpc.HelloServiceImplBase() {
    override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloResponse?>) {
        GlobalScope.launch {
            println("🌀 Thread name: ${Thread.currentThread().name}")
            val message = withContext(Dispatchers.IO) {
                delay(1000)
                "Coroutine: Hello ${request.name}"
            }

            val response = HelloResponse.newBuilder()
                .setMessage(message)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }
}

fun main() {
    val server: Server = ServerBuilder
        .forPort(1995)
        .addService(GrpcServer())
        .build()

    println("🚀 gRPC Kotlin server is running on port 1995...")
    server.start()
    server.awaitTermination()
}