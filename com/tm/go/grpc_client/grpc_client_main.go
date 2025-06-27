package main

import (
	"context"
	"log"
	"time"

	pb "com.tm.go/model/grpc/message"
	"google.golang.org/grpc"
)

func main() {
	conn, err := grpc.Dial("localhost:1995", grpc.WithInsecure())
	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()

	client := pb.NewHelloServiceClient(conn)

	go func() {
		for {
			time.Sleep(1 * time.Second)
			ctx, cancel := context.WithTimeout(context.Background(), time.Second)
			resp, err := client.SayHello(ctx, &pb.HelloRequest{Name: "Toàn"})
			cancel()

			if err != nil {
				log.Printf("could not greet: %v", err)
				continue
			}
			log.Printf("Response from server: %s", resp.Message)
		}
	}()

	select {}
}
