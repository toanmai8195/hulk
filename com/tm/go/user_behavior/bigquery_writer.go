package user_behavior

import (
	"context"
	"fmt"
	"sync"
	"time"

	"cloud.google.com/go/bigquery"
)

const (
	// BigQuery batch configuration
	DefaultBatchSize     = 500
	DefaultFlushInterval = 10 * time.Second
)

// BigQueryWriter handles batch writing events to BigQuery
type BigQueryWriter struct {
	client        *bigquery.Client
	dataset       string
	eventTable    string
	summaryTable  string
	eventBatch    []UserEvent
	summaryBatch  []SessionSummary
	eventMu       sync.Mutex
	summaryMu     sync.Mutex
	batchSize     int
	flushInterval time.Duration
	ctx           context.Context
	cancel        context.CancelFunc
	metrics       *BQMetrics
}

// BQMetrics tracks BigQuery write performance
type BQMetrics struct {
	EventsWritten   int64
	SummariesWritten int64
	ErrorCount      int64
	BatchCount      int64
	mu              sync.Mutex
}

// NewBigQueryWriter creates a new BigQuery writer
func NewBigQueryWriter(projectID, dataset, eventTable, summaryTable string) (*BigQueryWriter, error) {
	ctx := context.Background()

	client, err := bigquery.NewClient(ctx, projectID)
	if err != nil {
		return nil, fmt.Errorf("failed to create BQ client: %w", err)
	}

	ctx, cancel := context.WithCancel(ctx)

	return &BigQueryWriter{
		client:        client,
		dataset:       dataset,
		eventTable:    eventTable,
		summaryTable:  summaryTable,
		eventBatch:    make([]UserEvent, 0, DefaultBatchSize),
		summaryBatch:  make([]SessionSummary, 0, DefaultBatchSize),
		batchSize:     DefaultBatchSize,
		flushInterval: DefaultFlushInterval,
		ctx:           ctx,
		cancel:        cancel,
		metrics:       &BQMetrics{},
	}, nil
}

// Start begins the BigQuery writer background workers
func (bw *BigQueryWriter) Start() {
	// Start periodic flush worker
	go bw.periodicFlushWorker()
}

// Stop gracefully stops the BigQuery writer
func (bw *BigQueryWriter) Stop() error {
	bw.cancel()

	// Flush remaining batches
	if err := bw.FlushEvents(); err != nil {
		return err
	}
	if err := bw.FlushSummaries(); err != nil {
		return err
	}

	return bw.client.Close()
}

// WriteEvent adds an event to the batch
func (bw *BigQueryWriter) WriteEvent(event UserEvent) error {
	bw.eventMu.Lock()
	defer bw.eventMu.Unlock()

	bw.eventBatch = append(bw.eventBatch, event)

	// Auto-flush if batch is full
	if len(bw.eventBatch) >= bw.batchSize {
		go func() {
			if err := bw.FlushEvents(); err != nil {
				fmt.Printf("Error flushing events: %v\n", err)
			}
		}()
	}

	return nil
}

// WriteSummary adds a session summary to the batch
func (bw *BigQueryWriter) WriteSummary(summary SessionSummary) error {
	bw.summaryMu.Lock()
	defer bw.summaryMu.Unlock()

	bw.summaryBatch = append(bw.summaryBatch, summary)

	// Auto-flush if batch is full
	if len(bw.summaryBatch) >= bw.batchSize {
		go func() {
			if err := bw.FlushSummaries(); err != nil {
				fmt.Printf("Error flushing summaries: %v\n", err)
			}
		}()
	}

	return nil
}

// FlushEvents writes all batched events to BigQuery
func (bw *BigQueryWriter) FlushEvents() error {
	bw.eventMu.Lock()
	defer bw.eventMu.Unlock()

	if len(bw.eventBatch) == 0 {
		return nil
	}

	// Create a copy to write
	batch := make([]UserEvent, len(bw.eventBatch))
	copy(batch, bw.eventBatch)

	// Clear the batch
	bw.eventBatch = bw.eventBatch[:0]

	// Write to BigQuery
	if err := bw.writeEventBatch(batch); err != nil {
		// On error, put events back (or send to DLQ)
		bw.eventBatch = append(bw.eventBatch, batch...)
		return err
	}

	return nil
}

// FlushSummaries writes all batched summaries to BigQuery
func (bw *BigQueryWriter) FlushSummaries() error {
	bw.summaryMu.Lock()
	defer bw.summaryMu.Unlock()

	if len(bw.summaryBatch) == 0 {
		return nil
	}

	// Create a copy to write
	batch := make([]SessionSummary, len(bw.summaryBatch))
	copy(batch, bw.summaryBatch)

	// Clear the batch
	bw.summaryBatch = bw.summaryBatch[:0]

	// Write to BigQuery
	if err := bw.writeSummaryBatch(batch); err != nil {
		// On error, put summaries back
		bw.summaryBatch = append(bw.summaryBatch, batch...)
		return err
	}

	return nil
}

// writeEventBatch writes a batch of events to BigQuery
func (bw *BigQueryWriter) writeEventBatch(events []UserEvent) error {
	inserter := bw.client.Dataset(bw.dataset).Table(bw.eventTable).Inserter()

	// Convert to BigQuery value savers
	var rows []*bigquery.StructSaver
	for i := range events {
		rows = append(rows, &bigquery.StructSaver{
			Schema: eventSchema(),
			Struct: &events[i],
		})
	}

	// Insert to BigQuery
	if err := inserter.Put(bw.ctx, rows); err != nil {
		bw.metrics.incrementError()
		return fmt.Errorf("failed to insert events to BQ: %w", err)
	}

	bw.metrics.incrementEvents(int64(len(events)))
	return nil
}

// writeSummaryBatch writes a batch of summaries to BigQuery
func (bw *BigQueryWriter) writeSummaryBatch(summaries []SessionSummary) error {
	inserter := bw.client.Dataset(bw.dataset).Table(bw.summaryTable).Inserter()

	// Convert to BigQuery value savers
	var rows []*bigquery.StructSaver
	for i := range summaries {
		rows = append(rows, &bigquery.StructSaver{
			Schema: summarySchema(),
			Struct: &summaries[i],
		})
	}

	// Insert to BigQuery
	if err := inserter.Put(bw.ctx, rows); err != nil {
		bw.metrics.incrementError()
		return fmt.Errorf("failed to insert summaries to BQ: %w", err)
	}

	bw.metrics.incrementSummaries(int64(len(summaries)))
	return nil
}

// periodicFlushWorker periodically flushes batches
func (bw *BigQueryWriter) periodicFlushWorker() {
	ticker := time.NewTicker(bw.flushInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			if err := bw.FlushEvents(); err != nil {
				fmt.Printf("Periodic flush events error: %v\n", err)
			}
			if err := bw.FlushSummaries(); err != nil {
				fmt.Printf("Periodic flush summaries error: %v\n", err)
			}

		case <-bw.ctx.Done():
			return
		}
	}
}

// GetMetrics returns current BigQuery write metrics
func (bw *BigQueryWriter) GetMetrics() BQMetrics {
	bw.metrics.mu.Lock()
	defer bw.metrics.mu.Unlock()

	return *bw.metrics
}

// incrementEvents increments event write count
func (m *BQMetrics) incrementEvents(count int64) {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.EventsWritten += count
	m.BatchCount++
}

// incrementSummaries increments summary write count
func (m *BQMetrics) incrementSummaries(count int64) {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.SummariesWritten += count
	m.BatchCount++
}

// incrementError increments error count
func (m *BQMetrics) incrementError() {
	m.mu.Lock()
	defer m.mu.Unlock()

	m.ErrorCount++
}

// eventSchema defines BigQuery schema for events
func eventSchema() bigquery.Schema {
	return bigquery.Schema{
		{Name: "event_id", Type: bigquery.StringFieldType, Required: true},
		{Name: "user_id", Type: bigquery.StringFieldType, Required: true},
		{Name: "session_id", Type: bigquery.StringFieldType, Required: true},
		{Name: "event_type", Type: bigquery.StringFieldType, Required: true},
		{Name: "timestamp", Type: bigquery.TimestampFieldType, Required: true},
		{Name: "screen_name", Type: bigquery.StringFieldType},
		{Name: "metadata", Type: bigquery.JSONFieldType},
	}
}

// summarySchema defines BigQuery schema for session summaries
func summarySchema() bigquery.Schema {
	return bigquery.Schema{
		{Name: "session_id", Type: bigquery.StringFieldType, Required: true},
		{Name: "user_id", Type: bigquery.StringFieldType, Required: true},
		{Name: "start_time", Type: bigquery.TimestampFieldType, Required: true},
		{Name: "end_time", Type: bigquery.TimestampFieldType, Required: true},
		{Name: "duration_seconds", Type: bigquery.IntegerFieldType, Required: true},
		{Name: "event_count", Type: bigquery.IntegerFieldType, Required: true},
		{Name: "unique_screens", Type: bigquery.IntegerFieldType},
		{Name: "action_counts", Type: bigquery.JSONFieldType},
		{Name: "has_anomaly", Type: bigquery.BooleanFieldType},
		{Name: "anomaly_types", Type: bigquery.StringFieldType, Repeated: true},
	}
}
