#!/bin/bash
set -e


SCRIPT_DIR=$(cd $(dirname "$0") && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

echo "🐳 Running docker-compose prometheus..."
docker compose -f $COMPOSE_FILE up -d prometheus

#echo "🐳 Running docker-compose node_exporter..."
#docker compose -f $COMPOSE_FILE up -d node_exporter

echo "🐳 Running docker-compose grafana..."
docker compose -f $COMPOSE_FILE up -d grafana

#echo "🔥 Building image with Bazel..."
#bazel run //com/tm/go/grpc:grpc_server_docker
#
#echo "🐳 Running docker-compose grpc_server..."
#docker compose -f $COMPOSE_FILE up -d grpc_server

#echo "🔥 Building image with Bazel..."
#bazel run //com/tm/kotlin/service/counter:counter_image_load
#
#echo "🐳 Running docker-compose counter_service..."
#docker compose -f $COMPOSE_FILE up -d counter_service

# echo "🔥 Building image with Bazel..."
# bazel run //com/tm/kotlin/service/coroutine/httpserver:httpserver_image_load

# echo "🔥 Building image with Bazel..."
# bazel run //com/tm/kotlin/service/coroutine/httpclient:httpclient_image_load

# echo "🐳 Running docker-compose httpserver_coroutine..."
# docker compose -f $COMPOSE_FILE up -d httpserver_coroutine

# echo "🐳 Running docker-compose httpclient_coroutine..."
# docker compose -f $COMPOSE_FILE up -d httpclient_coroutine

echo "🔥 Building checkfriends image with Bazel..."
bazel run //com/tm/go/momo/httpclient:checkfriends_docker

 echo "🐳 Running docker-compose checkfriends..."
 docker compose -f $COMPOSE_FILE up -d checkfriends

#echo "🕒 Setting created time for checkfriends image..."
#CID=$(docker create com.tm.go.checkfriends:v1.0.0)
#docker commit --change "LABEL org.opencontainers.image.created=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
#  $CID com.tm.go.checkfriends:v1.0.0
#docker rm $CID

#echo "🐳 Running docker-compose checkfriends..."
#docker rm -f checkfriends-app 2>/dev/null || true
#docker compose -f $COMPOSE_FILE up -d checkfriends