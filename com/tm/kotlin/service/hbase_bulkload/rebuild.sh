#!/bin/bash

set -e  # Exit on error

echo "=================================================="
echo "  HBase BulkLoad - Rebuild and Reload Image"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

IMAGE_NAME="com.tm.kotlin.hbase_bulkload:latest"

# Read phase argument
PHASE="${1:-}"

if [ -z "$PHASE" ]; then
  echo -e "${RED}❌ Error: PHASE argument is required${NC}"
  echo ""
  echo "Usage:"
  echo "  $0 <phase>"
  echo ""
  echo "Examples:"
  echo "  $0 1           # Run phase 1 only"
  echo "  $0 1,2,3       # Run phases 1, 2, 3"
  echo "  $0 all         # Run all phases"
  echo ""
  exit 1
fi

echo -e "${GREEN}📋 Phase to run: $PHASE${NC}"
echo ""

echo -e "${YELLOW}[1/4] Stopping container...${NC}"
docker stop hbase_bulkload 2>/dev/null || true

echo -e "${YELLOW}[2/4] Removing old image...${NC}"
docker rmi -f ${IMAGE_NAME} 2>/dev/null || echo "  (Image not found, skipping)"

echo -e "${YELLOW}[3/4] Building and loading image with Bazel...${NC}"
cd /Users/toanmai/Documents/code/hulk
bazel run //com/tm/kotlin/service/hbase_bulkload:hbase_bulkload_image_load

echo -e "${YELLOW}[4/4] Restarting container with PHASE=$PHASE...${NC}"
cd /Users/toanmai/Documents/code/hulk/com/tm/docker
PHASE="$PHASE" docker-compose up -d hbase_bulkload

echo ""
echo -e "${GREEN}✅ Done! Image rebuilt and container restarted.${NC}"
echo -e "${GREEN}   Running phase(s): $PHASE${NC}"
echo ""
echo "View logs with:"
echo "  docker-compose logs -f hbase_bulkload"
echo ""
