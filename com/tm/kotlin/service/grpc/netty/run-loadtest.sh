#!/bin/bash
set -e

echo "🚀 Starting gRPC Load Test Setup..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Prometheus is running
echo -e "${YELLOW}1. Checking Prometheus...${NC}"
if docker ps | grep -q prometheus; then
    echo -e "${GREEN}✓ Prometheus is running${NC}"
else
    echo "Starting Prometheus..."
    cd ../../docker
    docker-compose up -d prometheus
    cd -
fi

# Build server
echo ""
echo -e "${YELLOW}2. Building gRPC server...${NC}"
cd ../../../..
bazel build //com/tm/kotlin/service/grpc/netty:grpc_compute_service
echo -e "${GREEN}✓ Server built${NC}"

# Build client
echo ""
echo -e "${YELLOW}3. Building load test client...${NC}"
bazel build //com/tm/kotlin/service/grpc/netty:load_test_client
echo -e "${GREEN}✓ Client built${NC}"

echo ""
echo -e "${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "Next steps:"
echo "1. Start server:  bazel run //com/tm/kotlin/service/grpc/netty:grpc_compute_service"
echo "2. Start client:  bazel run //com/tm/kotlin/service/grpc/netty:load_test_client"
echo ""
echo "Metrics endpoints:"
echo "- Server:     http://localhost:9091/metrics"
echo "- Client:     http://localhost:9092/metrics"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana:    http://localhost:3000"
echo ""
