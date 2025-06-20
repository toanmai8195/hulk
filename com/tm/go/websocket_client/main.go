package main

import (
	"log"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
)

const (
	serverURL  = "ws://localhost:8080/go/ws"
	clientID   = "client-123"
	payloadMsg = "Hello from Go WebSocket client!"
)

type Request struct {
	Payload string `json:"payload"`
}

type Response struct {
	Receiver string `json:"receiver"`
	Payload  string `json:"payload"`
}

func main() {
	header := http.Header{}
	header.Add("X-Client-ID", clientID)

	conn, _, err := websocket.DefaultDialer.Dial(serverURL, header)
	if err != nil {
		log.Fatal("Dial error:", err)
	}
	defer conn.Close()
	log.Println("Connected to WebSocket server as", clientID)

	request := Request{Payload: payloadMsg}

	go func() {
		for {
			if err := conn.WriteJSON(request); err != nil {
				log.Println("WriteJSON error:", err)
				return
			}
			log.Println("Sent:", payloadMsg)
			time.Sleep(1 * time.Second)
		}
	}()

	go func() {
		for {
			var response Response
			if err := conn.ReadJSON(&response); err != nil {
				log.Println("ReadJSON error:", err)
				return
			}
			log.Printf("Received response: Receiver=%s Payload=%s\n", response.Receiver, response.Payload)
		}
	}()

	select {}
}

// bazel run //com/tm/go/websocket_client
