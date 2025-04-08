package main

import (
	"encoding/json"
	"log"
	"net/http"
	"runtime"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

const (
	MaxWorkers     = 100
	ChannelBufSize = 1000
)

type Client struct {
	Conn  *websocket.Conn
	Mutex sync.Mutex
}

var (
	upgrader    = websocket.Upgrader{CheckOrigin: func(r *http.Request) bool { return true }}
	mu          sync.RWMutex
	connections = make(map[string]*Client)
	broadcast   = make(chan *Request, ChannelBufSize)

	// Prometheus Metrics
	activeConnections = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "websocket_active_connections",
		Help: "Current number of active WebSocket connections",
	})
	cpuUsage = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "websocket_cpu_usage",
		Help: "CPU usage percentage",
	})
	memoryUsage = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "websocket_memory_usage",
		Help: "Memory usage in MB",
	})
)

func init() {
	prometheus.MustRegister(activeConnections, cpuUsage, memoryUsage)
}

type Request struct {
	Caller  string
	Payload string `json:"payload"`
}

type Response struct {
	Receiver string
	Payload  string `json:"payload"`
}

func handleConnections(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("WebSocket Upgrade Error:", err)
		return
	}
	defer conn.Close()

	clientID := r.Header.Get("X-Client-ID")
	if clientID == "" {
		log.Println("Missing client ID in header")
		return
	}

	mu.Lock()
	if len(connections) >= 10000 {
		log.Println("Max connections reached!")
		mu.Unlock()
		return
	}
	connections[clientID] = &Client{Conn: conn}
	activeConnections.Inc() // Tăng số lượng kết nối
	mu.Unlock()

	log.Printf("Client %s connected", clientID)

	for {
		_, msg, err := conn.ReadMessage()
		if err != nil {
			log.Println("Client disconnected:", clientID, err)
			break
		}

		event := &Request{}
		if err := json.Unmarshal(msg, event); err != nil {
			log.Println("Failed to parse event:", err)
			continue
		}

		event.Caller = clientID

		select {
		case broadcast <- event:
		default:
			log.Println("Broadcast channel full, dropping message from", clientID)
		}
	}

	mu.Lock()
	delete(connections, clientID)
	activeConnections.Dec() // Giảm số lượng kết nối
	mu.Unlock()
}

func worker() {
	for event := range broadcast {
		mu.RLock()
		client, exists := connections[event.Caller]
		mu.RUnlock()

		if !exists {
			log.Println("Connection not found for", event.Caller)
			continue
		}

		response := Response{
			Receiver: event.Caller,
			Payload:  event.Payload,
		}

		msg, err := json.Marshal(response)
		if err != nil {
			log.Println("Failed to marshal response:", err)
			continue
		}

		go func(client *Client, msg []byte) {
			client.Mutex.Lock()
			defer client.Mutex.Unlock()

			err := client.Conn.WriteMessage(websocket.TextMessage, msg)
			if err != nil {
				log.Println("WriteMessage error for", event.Caller, ":", err)
				mu.Lock()
				client.Conn.Close()
				delete(connections, event.Caller)
				activeConnections.Dec()
				mu.Unlock()
			}
		}(client, msg)
	}
}

func monitorMetrics() {
	for {
		// Đo RAM
		var memStats runtime.MemStats
		runtime.ReadMemStats(&memStats)
		memoryUsage.Set(float64(memStats.Alloc) / 1024 / 1024)

		time.Sleep(5 * time.Second)
	}
}

func main() {
	http.HandleFunc("/go/ws", handleConnections)
	http.Handle("/metrics", promhttp.Handler()) // Endpoint để Prometheus thu thập số liệu

	for i := 0; i < MaxWorkers; i++ {
		go worker()
	}

	go monitorMetrics() // Bắt đầu đo CPU & RAM

	port := ":8080"
	log.Println("WebSocket server started on port", port)
	if err := http.ListenAndServe(port, nil); err != nil {
		log.Fatal("ListenAndServe:", err)
	}
}

//bazel run //om/tm/go:server
