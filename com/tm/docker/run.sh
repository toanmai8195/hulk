#!/bin/bash
set -e

echo "🔥 Building image with Bazel..."
bazel run //com/tm/go/grpc:grpc_server_docker

echo "🐳 Running docker-compose grpc_server..."
docker compose up grpc_server