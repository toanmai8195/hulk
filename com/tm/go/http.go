package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"
)

// RequestData represents the data structure for HTTP requests
type RequestData struct {
	Message string `json:"message"`
	ID      int    `json:"id"`
	Data    string `json:"data"`
}

// ResponseData represents the data structure for HTTP responses
type ResponseData struct {
	Status  string      `json:"status"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
	Time    time.Time   `json:"time"`
}

// HTTPClient provides methods for making HTTP requests
type HTTPClient struct {
	client  *http.Client
	baseURL string
}

// NewHTTPClient creates a new HTTP client instance
func NewHTTPClient(baseURL string, timeout time.Duration) *HTTPClient {
	return &HTTPClient{
		client: &http.Client{
			Timeout: timeout,
		},
		baseURL: baseURL,
	}
}

// PostJSON sends a POST request with JSON data
func (c *HTTPClient) PostJSON(endpoint string, data interface{}) (*ResponseData, error) {
	jsonData, err := json.Marshal(data)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal JSON: %w", err)
	}

	url := c.baseURL + endpoint
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("User-Agent", "Hulk-HTTP-Client/1.0")

	resp, err := c.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var response ResponseData
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return &response, nil
}

// PostForm sends a POST request with form data
func (c *HTTPClient) PostForm(endpoint string, formData map[string]string) (*ResponseData, error) {
	// Convert form data to URL-encoded format
	values := make([]string, 0, len(formData)*2)
	for key, value := range formData {
		values = append(values, key+"="+value)
	}

	url := c.baseURL + endpoint
	req, err := http.NewRequest("POST", url, bytes.NewBufferString(fmt.Sprintf("%s", values)))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := c.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var response ResponseData
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return &response, nil
}

// HTTPServer provides HTTP server functionality
type HTTPServer struct {
	port string
	mux  *http.ServeMux
}

// NewHTTPServer creates a new HTTP server instance
func NewHTTPServer(port string) *HTTPServer {
	return &HTTPServer{
		port: port,
		mux:  http.NewServeMux(),
	}
}

// RegisterHandlers registers all HTTP handlers
func (s *HTTPServer) RegisterHandlers() {
	s.mux.HandleFunc("/api/post", s.handlePost)
	s.mux.HandleFunc("/api/health", s.handleHealth)
	s.mux.HandleFunc("/", s.handleDefault)
}

// Start starts the HTTP server
func (s *HTTPServer) Start() error {
	log.Printf("Starting HTTP server on port %s", s.port)
	return http.ListenAndServe(":"+s.port, s.mux)
}

// handlePost handles POST requests
func (s *HTTPServer) handlePost(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Read request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Failed to read request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	// Parse JSON request
	var requestData RequestData
	if err := json.Unmarshal(body, &requestData); err != nil {
		http.Error(w, "Invalid JSON format", http.StatusBadRequest)
		return
	}

	// Process the request
	response := ResponseData{
		Status:  "success",
		Message: "Data received successfully",
		Data:    requestData,
		Time:    time.Now(),
	}

	// Set response headers
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)

	// Send response
	json.NewEncoder(w).Encode(response)
}

// handleHealth handles health check requests
func (s *HTTPServer) handleHealth(w http.ResponseWriter, r *http.Request) {
	response := ResponseData{
		Status:  "healthy",
		Message: "Server is running",
		Time:    time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// handleDefault handles all other requests
func (s *HTTPServer) handleDefault(w http.ResponseWriter, r *http.Request) {
	response := ResponseData{
		Status:  "info",
		Message: "Welcome to Hulk HTTP Server",
		Time:    time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// Example usage functions
func exampleHTTPClient() {
	// Create HTTP client
	client := NewHTTPClient("http://localhost:8080", 30*time.Second)

	// Example POST request with JSON
	requestData := RequestData{
		Message: "Hello from Hulk!",
		ID:      123,
		Data:    "Sample data",
	}

	response, err := client.PostJSON("/api/post", requestData)
	if err != nil {
		log.Printf("Error making POST request: %v", err)
		return
	}

	log.Printf("Response: %+v", response)
}

func exampleHTTPServer() {
	// Create and start HTTP server
	server := NewHTTPServer("8080")
	server.RegisterHandlers()

	log.Fatal(server.Start())
}

// Example functions for demonstration
// To use these functions, import this package and call them from your main function
