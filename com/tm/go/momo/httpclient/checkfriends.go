package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Client represents the HTTP client for MoMo APIs
type Client struct {
	baseURL    string
	httpClient *http.Client
	env        string
	token      string
}

// NewClient creates a new MoMo HTTP client
func NewClient(baseURL, env, token string) *Client {
	return &Client{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		env:   env,
		token: token,
	}
}

// MutualLoadRequest represents the request payload for mutual friends load
type MutualLoadRequest struct {
	ActorID   string `json:"actor_id"`
	PartnerID string `json:"partner_id"`
	Offset    int    `json:"offset"`
	Limit     int    `json:"limit"`
}

// MutualLoadResponse represents the response from mutual friends load API
type MutualLoadResponse struct {
	Actor   string `json:"actor"`
	Success bool   `json:"success"`
	JSON    struct {
		Actor     string   `json:"actor"`
		MutualIDs []string `json:"mutual_ids"`
		Total     int      `json:"total"`
	} `json:"json"`
}

// LoadMutualFriends calls the mutual friends load API
func (c *Client) LoadMutualFriends(req MutualLoadRequest) (*MutualLoadResponse, error) {
	// Prepare request body
	reqBody, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	// Create HTTP request
	url := fmt.Sprintf("%s/internal//helios-social-relationships/v1/mutual/load", c.baseURL)
	httpReq, err := http.NewRequest("POST", url, bytes.NewBuffer(reqBody))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	// Set headers
	httpReq.Header.Set("env", c.env)
	httpReq.Header.Set("Authorization", fmt.Sprintf("Bearer %s", c.token))
	httpReq.Header.Set("Content-Type", "application/json")

	// Make the request
	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("failed to make request: %w", err)
	}
	defer resp.Body.Close()

	// Read response body
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	// Check HTTP status code
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d, body: %s", resp.StatusCode, string(respBody))
	}

	// Parse response
	var response MutualLoadResponse
	if err := json.Unmarshal(respBody, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return &response, nil
}

// LoadMutualFriendsWithDefaults calls the API with default values
func (c *Client) LoadMutualFriendsWithDefaults(actorID, partnerID string) (*MutualLoadResponse, error) {
	req := MutualLoadRequest{
		ActorID:   actorID,
		PartnerID: partnerID,
		Offset:    0,
		Limit:     20,
	}
	return c.LoadMutualFriends(req)
}

func main() {
	// Seed random number generator
	rand.Seed(time.Now().UnixNano())

	// Prometheus metrics
	requestCounter := prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "api_requests_total",
			Help: "Total number of API requests",
		},
		[]string{"status", "client_id"},
	)

	requestDuration := prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "api_request_duration_seconds",
			Help:    "API request duration in seconds",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"client_id"},
	)

	rpsCounter := prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "api_requests_per_second_total",
			Help: "Total number of requests per second (rate limit counter)",
		},
	)

	// Debug metric to verify metrics endpoint is working
	debugCounter := prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "debug_counter_total",
			Help: "Debug counter to verify metrics endpoint",
		},
	)

	// Register metrics
	prometheus.MustRegister(requestCounter)
	prometheus.MustRegister(requestDuration)
	prometheus.MustRegister(rpsCounter)
	prometheus.MustRegister(debugCounter)

	// Increment debug counter immediately to verify metrics are working
	debugCounter.Inc()

	// Start Prometheus HTTP server
	go func() {
		// Health check endpoint
		http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusOK)
			w.Write([]byte("OK"))
		})

		// Metrics endpoint
		http.Handle("/metrics", promhttp.Handler())

		fmt.Println("Prometheus metrics server started at 0.0.0.0:8080")
		fmt.Println("Endpoints:")
		fmt.Println("  - Health: http://0.0.0.0:8080/health")
		fmt.Println("  - Metrics: http://0.0.0.0:8080/metrics")
		fmt.Println("This endpoint can be scraped by external Prometheus instances")

		if err := http.ListenAndServe("0.0.0.0:8080", nil); err != nil {
			fmt.Printf("Error starting metrics server: %v\n", err)
		}
	}()

	// Configuration
	rps := 200
	numClients := 20
	reqsPerClient := rps / numClients
	interval := time.Second / time.Duration(reqsPerClient)

	baseURL := "https://s.dev.mservice.io"
	env := "dev"
	token := "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJoZWxpb3MiLCJzdWIiOiJ0b2FuLm1haTEiLCJyb2xlcyI6WyJBRE1JTiJdLCJhdWQiOiJDSEFUIiwiaWF0IjoxNzQ1ODE0MTcxLCJ0a3QiOiJVU0VSIiwianRpIjoiNWVjYjYwNmEtOTc1My00ZGRlLWEzNTMtMTE5ZGEyNzZlMzdiIiwic3ZjIjpbIiJdLCJhcGlzIjpbIiJdLCJ2IjoxfQ.5NfRG0rBkFRXg7QSBqzGeB-2JHUc5PDckJjJX6RLEl0"

	fmt.Printf("Starting %d parallel API clients running forever with rate limit: %v (%d RPS)...\n", numClients, interval, rps)
	fmt.Println("Press Ctrl+C to stop...")
	fmt.Println("Metrics available at http://localhost:8080/metrics")

	// Launch all clients to run forever
	for i := 0; i < numClients; i++ {
		go func(clientID int) {
			// Create client
			client := NewClient(baseURL, env, token)

			// Create a ticker for this client
			ticker := time.NewTicker(interval)
			defer ticker.Stop()

			// Run forever
			for {
				// Wait for the ticker signal before making the API call
				<-ticker.C

				// Increment RPS counter
				rpsCounter.Inc()

				// Generate random actor and partner IDs
				actorID := fmt.Sprintf("0166%07d", rand.Intn(10000))  // 01660000000 to 01660010000
				partnerID := fmt.Sprintf("0166%07d", rand.Intn(1000)) // 01660000000 to 01660010000

				// Start timing
				start := time.Now()

				// Call API
				resp, err := client.LoadMutualFriendsWithDefaults(actorID, partnerID)

				// Record metrics
				duration := time.Since(start).Seconds()
				requestDuration.WithLabelValues(fmt.Sprintf("client_%d", clientID)).Observe(duration)

				if err != nil {
					requestCounter.WithLabelValues("error", fmt.Sprintf("client_%d", clientID)).Inc()
					fmt.Printf("Client %d (Actor: %s, Partner: %s) - Error: %v\n", clientID, actorID, partnerID, err)
					continue
				}

				// Record success/failure
				if resp.Success {
					requestCounter.WithLabelValues("success", fmt.Sprintf("client_%d", clientID)).Inc()
					fmt.Printf("Client %d (Actor: %s, Partner: %s) - ✅ Found %d mutual friends (%.3fs)\n",
						clientID, actorID, partnerID, resp.JSON.Total, duration)
				} else {
					requestCounter.WithLabelValues("failed", fmt.Sprintf("client_%d", clientID)).Inc()
					fmt.Printf("Client %d (Actor: %s, Partner: %s) - ❌ Not successful (%.3fs)\n",
						clientID, actorID, partnerID, duration)
				}
			}
		}(i)
	}

	// Keep the main goroutine alive forever
	select {}
}
