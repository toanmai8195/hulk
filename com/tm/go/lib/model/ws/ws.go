package ws_model

import (
	"sync"

	"github.com/gorilla/websocket"
)

// Define object
type Client struct {
	Conn  *websocket.Conn
	Mutex sync.Mutex
}

type Request struct {
	Caller  string
	Payload string `json:"payload"`
}

type Response struct {
	Receiver string
	Payload  string `json:"payload"`
}
