package user_behavior

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func Main() {
	// Configuration
	config := EventCollectorConfig{
		HBaseHost:           getEnv("HBASE_HOST", "localhost"),
		HBaseTable:          getEnv("HBASE_TABLE", "user_behavior_events"),
		BQProjectID:         getEnv("BQ_PROJECT_ID", "your-project-id"),
		BQDataset:           getEnv("BQ_DATASET", "user_behavior"),
		BQEventTable:        getEnv("BQ_EVENT_TABLE", "events"),
		BQSummaryTable:      getEnv("BQ_SUMMARY_TABLE", "session_summaries"),
		EventBufferSize:     10000,
		NumSessionWorkers:   10,
		NumHBaseWorkers:     20,
		AggregationInterval: 5 * time.Minute,
	}

	// Create event collector
	collector, err := NewEventCollector(config)
	if err != nil {
		log.Fatalf("Failed to create event collector: %v", err)
	}

	// Start the system
	collector.Start(config)

	// Setup HTTP server for receiving events and queries
	setupHTTPServer(collector)

	// Wait for shutdown signal
	waitForShutdown(collector)
}

func setupHTTPServer(collector *EventCollector) {
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	http.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) {
		metrics := collector.GetMetrics()
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{
			"hbase": {
				"writes": %d,
				"errors": %d
			},
			"bigquery": {
				"events_written": %d,
				"summaries_written": %d,
				"errors": %d,
				"batches": %d
			}
		}`,
			metrics.HBase.WriteCount,
			metrics.HBase.ErrorCount,
			metrics.BigQuery.EventsWritten,
			metrics.BigQuery.SummariesWritten,
			metrics.BigQuery.ErrorCount,
			metrics.BigQuery.BatchCount,
		)
	})

	// Example: Track event endpoint
	http.HandleFunc("/track", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		userID := r.URL.Query().Get("user_id")
		sessionID := r.URL.Query().Get("session_id")
		eventType := EventType(r.URL.Query().Get("event_type"))
		screenName := r.URL.Query().Get("screen_name")

		if userID == "" || sessionID == "" || eventType == "" {
			http.Error(w, "Missing required parameters", http.StatusBadRequest)
			return
		}

		err := collector.TrackEvent(userID, sessionID, eventType, screenName, nil)
		if err != nil {
			http.Error(w, fmt.Sprintf("Error tracking event: %v", err), http.StatusInternalServerError)
			return
		}

		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Event tracked"))
	})

	// Create session endpoint
	http.HandleFunc("/session/create", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		userID := r.URL.Query().Get("user_id")
		if userID == "" {
			http.Error(w, "Missing user_id", http.StatusBadRequest)
			return
		}

		sessionID := collector.CreateSession(userID)

		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{"session_id": "%s"}`, sessionID)
	})

	// Close session endpoint
	http.HandleFunc("/session/close", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		sessionID := r.URL.Query().Get("session_id")
		if sessionID == "" {
			http.Error(w, "Missing session_id", http.StatusBadRequest)
			return
		}

		collector.CloseSession(sessionID)

		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Session closed"))
	})

	// Get session analysis endpoint
	http.HandleFunc("/session/analysis", func(w http.ResponseWriter, r *http.Request) {
		sessionID := r.URL.Query().Get("session_id")
		if sessionID == "" {
			http.Error(w, "Missing session_id", http.StatusBadRequest)
			return
		}

		analysis, err := collector.GetSessionAnalysis(sessionID)
		if err != nil {
			http.Error(w, fmt.Sprintf("Error getting analysis: %v", err), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, `{
			"session_id": "%s",
			"event_count": %d,
			"most_used_actions": %d,
			"common_patterns": %d,
			"anomalies": %d,
			"repeated_patterns": %d
		}`,
			analysis.SessionID,
			analysis.EventCount,
			len(analysis.MostUsedActions),
			len(analysis.CommonPatterns),
			len(analysis.Anomalies),
			len(analysis.RepeatedPatterns),
		)
	})

	port := getEnv("PORT", "8080")
	go func() {
		log.Printf("HTTP server listening on port %s", port)
		if err := http.ListenAndServe(":"+port, nil); err != nil {
			log.Fatalf("HTTP server error: %v", err)
		}
	}()
}

func waitForShutdown(collector *EventCollector) {
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	<-sigChan

	log.Println("Shutdown signal received, stopping gracefully...")

	if err := collector.Stop(); err != nil {
		log.Printf("Error during shutdown: %v", err)
	}

	log.Println("Shutdown complete")
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
