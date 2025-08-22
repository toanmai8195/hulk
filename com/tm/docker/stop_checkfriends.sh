#!/bin/bash
set -e

echo "🛑 Stopping checkfriends container..."
docker stop checkfriends-app 2>/dev/null || echo "Container not running"

echo "🧹 Cleaning up checkfriends container..."
docker rm checkfriends-app 2>/dev/null || echo "Container already removed"

echo "✅ Checkfriends application stopped and cleaned up!"
