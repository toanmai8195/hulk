package user_behavior

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/tsuna/gohbase"
	"github.com/tsuna/gohbase/hrpc"
)

const (
	// HBase table configuration
	DefaultTableName   = "user_behavior_events"
	DefaultColumnFamily = "e" // events
)

// HBaseWriter handles writing events to HBase
type HBaseWriter struct {
	client       gohbase.Client
	tableName    string
	columnFamily string
	writeChannel chan UserEvent
	mu           sync.RWMutex
	ctx          context.Context
	cancel       context.CancelFunc
	metrics      *HBaseMetrics
}

// HBaseMetrics tracks HBase write performance
type HBaseMetrics struct {
	WriteCount    int64
	ErrorCount    int64
	TotalDuration time.Duration
	mu            sync.Mutex
}

// NewHBaseWriter creates a new HBase writer
func NewHBaseWriter(hbaseHost string, tableName string, bufferSize int) *HBaseWriter {
	ctx, cancel := context.WithCancel(context.Background())

	if tableName == "" {
		tableName = DefaultTableName
	}

	return &HBaseWriter{
		client:       gohbase.NewClient(hbaseHost),
		tableName:    tableName,
		columnFamily: DefaultColumnFamily,
		writeChannel: make(chan UserEvent, bufferSize),
		ctx:          ctx,
		cancel:       cancel,
		metrics:      &HBaseMetrics{},
	}
}

// Start begins the HBase writer workers
func (hw *HBaseWriter) Start(numWorkers int) {
	for i := 0; i < numWorkers; i++ {
		go hw.writeWorker()
	}
}

// Stop gracefully stops the HBase writer
func (hw *HBaseWriter) Stop() {
	hw.cancel()
	close(hw.writeChannel)
	hw.client.Close()
}

// WriteEvent queues an event for writing to HBase
func (hw *HBaseWriter) WriteEvent(event UserEvent) error {
	select {
	case hw.writeChannel <- event:
		return nil
	case <-hw.ctx.Done():
		return hw.ctx.Err()
	default:
		return ErrEventChannelFull
	}
}

// writeWorker processes events from the write channel
func (hw *HBaseWriter) writeWorker() {
	for {
		select {
		case event, ok := <-hw.writeChannel:
			if !ok {
				return
			}

			if err := hw.writeEventToHBase(event); err != nil {
				// Log error - in production, you might want to retry or use a dead letter queue
				fmt.Printf("Error writing event to HBase: %v\n", err)
				hw.metrics.incrementError()
			} else {
				hw.metrics.incrementSuccess(0) // Add actual duration if measured
			}

		case <-hw.ctx.Done():
			return
		}
	}
}

// writeEventToHBase writes a single event to HBase
func (hw *HBaseWriter) writeEventToHBase(event UserEvent) error {
	start := time.Now()

	// Row key design: sessionId_timestamp_eventId
	// This allows efficient range scans for session events
	rowKey := fmt.Sprintf("%s_%d_%s",
		event.SessionID,
		event.Timestamp.UnixNano(),
		event.EventID,
	)

	// Serialize event data
	eventJSON, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	// Prepare HBase values
	values := map[string]map[string][]byte{
		hw.columnFamily: {
			"event_id":    []byte(event.EventID),
			"user_id":     []byte(event.UserID),
			"session_id":  []byte(event.SessionID),
			"event_type":  []byte(event.EventType),
			"timestamp":   []byte(fmt.Sprintf("%d", event.Timestamp.Unix())),
			"screen_name": []byte(event.ScreenName),
			"full_data":   eventJSON,
		},
	}

	// Create put request
	putRequest, err := hrpc.NewPutStr(hw.ctx, hw.tableName, rowKey, values)
	if err != nil {
		return fmt.Errorf("failed to create put request: %w", err)
	}

	// Write to HBase
	_, err = hw.client.Put(putRequest)
	if err != nil {
		return fmt.Errorf("failed to put to HBase: %w", err)
	}

	duration := time.Since(start)
	hw.metrics.incrementSuccess(duration)

	return nil
}

// GetSessionEvents retrieves all events for a session from HBase
func (hw *HBaseWriter) GetSessionEvents(sessionID string) ([]UserEvent, error) {
	// Create scan with prefix (sessionId_)
	startRow := fmt.Sprintf("%s_", sessionID)
	endRow := fmt.Sprintf("%s_~", sessionID) // ~ is lexicographically after numbers

	scanRequest, err := hrpc.NewScanRangeStr(
		hw.ctx,
		hw.tableName,
		startRow,
		endRow,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create scan request: %w", err)
	}

	scanner := hw.client.Scan(scanRequest)
	var events []UserEvent

	for {
		result, err := scanner.Next()
		if err != nil {
			if err.Error() == "EOF" {
				break
			}
			return nil, fmt.Errorf("scan error: %w", err)
		}

		// Extract full_data column
		for _, cell := range result.Cells {
			if string(cell.Qualifier) == "full_data" {
				var event UserEvent
				if err := json.Unmarshal(cell.Value, &event); err != nil {
					continue
				}
				events = append(events, event)
			}
		}
	}

	return events, nil
}

// GetMetrics returns current HBase write metrics
func (hw *HBaseWriter) GetMetrics() HBaseMetrics {
	hw.metrics.mu.Lock()
	defer hw.metrics.mu.Unlock()

	return *hw.metrics
}

// incrementSuccess increments successful write count
func (m *HBaseMetrics) incrementSuccess(duration time.Duration) {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.WriteCount++
	m.TotalDuration += duration
}

// incrementError increments error count
func (m *HBaseMetrics) incrementError() {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.ErrorCount++
}
