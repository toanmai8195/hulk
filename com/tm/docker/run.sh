#!/bin/bash
set -e

echo "🐳 Running docker-compose prometheus..."
docker compose up -d prometheus

#echo "🐳 Running docker-compose node_exporter..."
#docker compose up -d node_exporter

echo "🐳 Running docker-compose grafana..."
docker compose up -d grafana

#echo "🔥 Building image with Bazel..."
#bazel run //com/tm/go/grpc:grpc_server_docker
#
#echo "🐳 Running docker-compose grpc_server..."
#docker compose up -d grpc_server

#echo "🔥 Building image with Bazel..."
#bazel run //com/tm/kotlin/service/counter:counter_image_load
#
#echo "🐳 Running docker-compose counter_service..."
#docker compose up -d counter_service

echo "🔥 Building image with Bazel..."
bazel run //com/tm/kotlin/service/coroutine/httpserver:httpserver_image_load

echo "🔥 Building image with Bazel..."
bazel run //com/tm/kotlin/service/coroutine/httpclient:httpclient_image_load

echo "🐳 Running docker-compose httpserver_coroutine..."
docker compose up -d httpserver_coroutine

echo "🐳 Running docker-compose httpclient_coroutine..."
docker compose up -d httpclient_coroutine

echo "🔥 Building Netty gRPC image with Bazel..."
bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_image_load

echo "🐳 Running docker-compose grpc_compute_netty_service..."
docker compose up -d grpc_compute_netty_service

echo "🔥 Building Vert.x gRPC image with Bazel..."
bazel run //com/tm/kotlin/service/grpc/vertx:vertx_grpc_compute_image_load

echo "🐳 Running docker-compose grpc_compute_vertx_service..."
docker compose up -d grpc_compute_vertx_service