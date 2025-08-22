#!/bin/bash
set -e

echo "🔥 Building checkfriends image with Bazel..."
bazel run //com/tm/go/momo/httpclient:checkfriends_docker

echo "🐳 Running checkfriends container..."
docker run -d -p 8080:8080 --name checkfriends-app --rm com.tm.go.checkfriends:v1.0.0

echo "✅ Checkfriends application is now running!"
echo "📊 Metrics available at: http://localhost:8080/metrics"
echo "🔍 Health check at: http://localhost:8080/health"
echo "🐳 Container logs: docker logs checkfriends-app"
echo "🛑 To stop: docker stop checkfriends-app"
