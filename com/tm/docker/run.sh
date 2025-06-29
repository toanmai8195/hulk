#!/bin/bash
set -e

echo "🐳 Running docker-compose prometheus..."
docker compose up -d prometheus

echo "🐳 Running docker-compose node_exporter..."
docker compose up -d node_exporter

echo "🐳 Running docker-compose grafana..."
docker compose up -d grafana

echo "🔥 Building image with Bazel..."
bazel run //com/tm/go/grpc:grpc_server_docker

echo "🐳 Running docker-compose grpc_server..."
docker compose up -d grpc_server