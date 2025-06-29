package main

import (
	"context"
	"log"
	"time"

	pb "com.tm.go/model/grpc/message"
	"google.golang.org/grpc"
)

const (
	numClients = 20                   // Số lượng goroutine song song
	delay      = 1 * time.Millisecond // Thời gian giữa các request mỗi goroutine
)

func main() {
	conn, err := grpc.Dial("localhost:1995", grpc.WithInsecure())
	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()

	client := pb.NewHelloServiceClient(conn)

	for i := 0; i < numClients; i++ {
		go runClient(client, i)
	}

	select {}
}

func runClient(client pb.HelloServiceClient, id int) {
	for {
		time.Sleep(delay)

		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		resp, err := client.SayHello(ctx, &pb.HelloRequest{Name: "Toàn"})
		cancel()

		if err != nil {
			log.Printf("[Client %d] could not greet: %v", id, err)
			continue
		}
		log.Printf("[Client %d] Response: %s", id, resp.Message)
	}
}
