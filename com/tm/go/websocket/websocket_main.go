// package main

// import (
// 	"com.tm.go/lib/model/ws_model"
// 	"encoding/json"
// 	"github.com/gorilla/websocket"
// 	"log"
// 	"net/http"
// 	"sync"
// )

// const (
// 	MaxWorkers     = 100
// 	ChannelBufSize = 1000
// 	Port           = ":8080"
// )

// var (
// 	upgrader = websocket.Upgrader{
// 		CheckOrigin: func(r *http.Request) bool { return true },
// 	}
// 	connections = make(map[string]*ws_model.Client)
// 	mutex       sync.RWMutex
// 	broadcast   = make(chan *ws_model.Request, ChannelBufSize)
// )

// func main() {
// 	http.HandleFunc("/go/ws", handler)

// 	for i := 0; i < MaxWorkers; i++ {
// 		go worker()
// 	}

// 	log.Println("WebSocket server started on port", Port)

// 	if err := http.ListenAndServe(Port, nil); err != nil {
// 		log.Fatal("ListenAndServe:", err)
// 	}
// }

// func handler(w http.ResponseWriter, r *http.Request) {
// 	conn, err := upgrader.Upgrade(w, r, nil)
// 	if err != nil {
// 		log.Println("WebSocket Upgrade Error:", err)
// 		return
// 	}

// 	defer conn.Close()

// 	clientID := r.Header.Get("X-Client-ID")
// 	if clientID == "" {
// 		log.Println("Missing client ID in header")
// 		return
// 	}

// 	mutex.Lock()
// 	if len(connections) >= 1000 {
// 		log.Println("Max connections reached!")
// 		mutex.Unlock()
// 		return
// 	}

// 	connections[clientID] = &ws_model.Client{Conn: conn}
// 	mutex.Unlock()

// 	log.Printf("Client %s connected", clientID)

// 	for {
// 		_, msg, err := conn.ReadMessage()
// 		if err != nil {
// 			log.Println("Client disconnected:", clientID, err)
// 			break
// 		}

// 		event := &ws_model.Request{}
// 		if err := json.Unmarshal(msg, event); err != nil {
// 			log.Println("Failed to parse event:", err)
// 			continue
// 		}

// 		event.Caller = clientID

// 		select {
// 		case broadcast <- event:
// 		default:
// 			log.Println("Broadcast channel full, dropping message from", clientID)
// 		}
// 	}

// 	mutex.Lock()
// 	delete(connections, clientID)
// 	mutex.Unlock()
// }

// func worker() {
// 	for event := range broadcast {
// 		mutex.RLock()
// 		client, exist := connections[event.Caller]
// 		mutex.RUnlock()

// 		if !exist {
// 			log.Println("Connection not found for", event.Caller)
// 			continue
// 		}

// 		log.Printf("Receive from client %s : %s ", event.Caller, event.Payload)

// 		response := ws_model.Response{
// 			Receiver: event.Caller,
// 			Payload:  event.Payload,
// 		}

// 		responseMessage, err := json.Marshal(response)
// 		if err != nil {
// 			log.Println("Failed to marshal response:", err)
// 			continue
// 		}

// 		go func(client *ws_model.Client, msg []byte) {
// 			client.Mutex.Lock()
// 			defer client.Mutex.Unlock()

// 			err := client.Conn.WriteMessage(websocket.TextMessage, msg)
// 			if err != nil {
// 				log.Println("WriteMessage error for", event.Caller, ":", err)
// 				mutex.Lock()
// 				client.Conn.Close()
// 				delete(connections, event.Caller)
// 				mutex.Unlock()
// 			}
// 		}(client, responseMessage)
// 	}
// }

// //bazel run //com/tm/go/websocket
