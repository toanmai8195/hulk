package main

import (
	"context"
	"log"
	"math"
	"math/rand/v2"
	"net"
	"net/http"
	"strconv"

	pb "com.tm.go/model/grpc/message"
	grpc_prometheus "github.com/grpc-ecosystem/go-grpc-prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"google.golang.org/grpc"
)

type server struct {
	pb.UnimplementedHelloServiceServer
}

func (s *server) SayHello(ctx context.Context, req *pb.HelloRequest) (*pb.HelloResponse, error) {
	loopSize := 20000
	consumeCPU(loopSize)
	return &pb.HelloResponse{Message: "Hello, " + req.Name + "(loopSize=" + strconv.Itoa(loopSize) + ")"}, nil
}

func consumeCPU(loopSize int) {
	for i := 0; i < loopSize; i++ {
		var x float64 = rand.Float64()
		x += math.Sqrt(x)
	}
}

func main() {
	lis, err := net.Listen("tcp", ":1995")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	s := grpc.NewServer(
		grpc.UnaryInterceptor(grpc_prometheus.UnaryServerInterceptor),
	)
	grpc_prometheus.EnableHandlingTimeHistogram()
	pb.RegisterHelloServiceServer(s, &server{})
	grpc_prometheus.Register(s)
	go func() {
		log.Println("Prometheus metrics available at :2112/metrics")
		http.Handle("/metrics", promhttp.Handler())
		if err := http.ListenAndServe(":2112", nil); err != nil {
			log.Fatalf("failed to start metrics HTTP server: %v", err)
		}
	}()
	log.Println("gRPC server listening on :1995")
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
