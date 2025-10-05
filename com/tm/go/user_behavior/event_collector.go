package user_behavior

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/google/uuid"
)

// EventCollector is the main orchestrator for the user behavior tracking system
type EventCollector struct {
	sessionManager *SessionManager
	hbaseWriter    *HBaseWriter
	bqWriter       *BigQueryWriter
	analyzer       *BehaviorAnalyzer
	aggregationJob *AggregationJob
	ctx            context.Context
	cancel         context.CancelFunc
	mu             sync.RWMutex
}

// EventCollectorConfig holds configuration for the event collector
type EventCollectorConfig struct {
	HBaseHost           string
	HBaseTable          string
	BQProjectID         string
	BQDataset           string
	BQEventTable        string
	BQSummaryTable      string
	EventBufferSize     int
	NumSessionWorkers   int
	NumHBaseWorkers     int
	AggregationInterval time.Duration
}

// NewEventCollector creates a new event collector
func NewEventCollector(config EventCollectorConfig) (*EventCollector, error) {
	ctx, cancel := context.WithCancel(context.Background())

	// Initialize components
	sessionManager := NewSessionManager(config.EventBufferSize)
	hbaseWriter := NewHBaseWriter(config.HBaseHost, config.HBaseTable, config.EventBufferSize)

	bqWriter, err := NewBigQueryWriter(
		config.BQProjectID,
		config.BQDataset,
		config.BQEventTable,
		config.BQSummaryTable,
	)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to create BigQuery writer: %w", err)
	}

	analyzer := NewBehaviorAnalyzer(sessionManager, hbaseWriter)
	aggregationJob := NewAggregationJob(
		sessionManager,
		hbaseWriter,
		bqWriter,
		analyzer,
		config.AggregationInterval,
	)

	return &EventCollector{
		sessionManager: sessionManager,
		hbaseWriter:    hbaseWriter,
		bqWriter:       bqWriter,
		analyzer:       analyzer,
		aggregationJob: aggregationJob,
		ctx:            ctx,
		cancel:         cancel,
	}, nil
}

// Start starts all components of the event collector
func (ec *EventCollector) Start(config EventCollectorConfig) {
	ec.sessionManager.Start(config.NumSessionWorkers)
	ec.hbaseWriter.Start(config.NumHBaseWorkers)
	ec.bqWriter.Start()
	ec.analyzer.Start()
	ec.aggregationJob.Start()

	fmt.Println("User Behavior Tracking System started successfully")
}

// Stop gracefully stops all components
func (ec *EventCollector) Stop() error {
	ec.cancel()

	ec.aggregationJob.Stop()
	ec.analyzer.Stop()
	ec.sessionManager.Stop()
	ec.hbaseWriter.Stop()

	if err := ec.bqWriter.Stop(); err != nil {
		return fmt.Errorf("error stopping BigQuery writer: %w", err)
	}

	fmt.Println("User Behavior Tracking System stopped successfully")
	return nil
}

// TrackEvent is the main entry point for tracking user events
func (ec *EventCollector) TrackEvent(
	userID string,
	sessionID string,
	eventType EventType,
	screenName string,
	metadata map[string]interface{},
) error {
	// Create event
	event := UserEvent{
		EventID:    uuid.New().String(),
		UserID:     userID,
		SessionID:  sessionID,
		EventType:  eventType,
		Timestamp:  time.Now(),
		ScreenName: screenName,
		Metadata:   metadata,
	}

	// Track in session manager
	if err := ec.sessionManager.TrackEvent(event); err != nil {
		return fmt.Errorf("failed to track event in session manager: %w", err)
	}

	// Write to HBase (async)
	if err := ec.hbaseWriter.WriteEvent(event); err != nil {
		return fmt.Errorf("failed to queue event for HBase: %w", err)
	}

	// Write to BigQuery (async batched)
	if err := ec.bqWriter.WriteEvent(event); err != nil {
		return fmt.Errorf("failed to queue event for BigQuery: %w", err)
	}

	return nil
}

// CreateSession creates a new session for a user
func (ec *EventCollector) CreateSession(userID string) string {
	return ec.sessionManager.CreateSession(userID)
}

// CloseSession explicitly closes a session
func (ec *EventCollector) CloseSession(sessionID string) {
	ec.sessionManager.CloseSession(sessionID)
}

// GetSessionAnalysis returns comprehensive analysis for a session
func (ec *EventCollector) GetSessionAnalysis(sessionID string) (*SessionAnalysisReport, error) {
	return ec.aggregationJob.AnalyzeSessionBehavior(sessionID)
}

// GetUserSessions returns all active sessions for a user
func (ec *EventCollector) GetUserSessions(userID string) []*Session {
	return ec.sessionManager.GetUserSessions(userID)
}

// GetMetrics returns metrics from all components
func (ec *EventCollector) GetMetrics() SystemMetrics {
	return SystemMetrics{
		HBase:    ec.hbaseWriter.GetMetrics(),
		BigQuery: ec.bqWriter.GetMetrics(),
	}
}

// SystemMetrics aggregates metrics from all components
type SystemMetrics struct {
	HBase    HBaseMetrics `json:"hbase"`
	BigQuery BQMetrics    `json:"bigquery"`
}
