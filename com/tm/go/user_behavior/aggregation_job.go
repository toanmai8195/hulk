package user_behavior

import (
	"context"
	"fmt"
	"sync"
	"time"
)

// AggregationJob handles periodic aggregation of session data for BigQuery
type AggregationJob struct {
	sessionManager *SessionManager
	hbaseReader    *HBaseWriter
	bqWriter       *BigQueryWriter
	analyzer       *BehaviorAnalyzer
	interval       time.Duration
	ctx            context.Context
	cancel         context.CancelFunc
	mu             sync.Mutex
}

// NewAggregationJob creates a new aggregation job
func NewAggregationJob(
	sessionManager *SessionManager,
	hbaseReader *HBaseWriter,
	bqWriter *BigQueryWriter,
	analyzer *BehaviorAnalyzer,
	interval time.Duration,
) *AggregationJob {
	ctx, cancel := context.WithCancel(context.Background())

	return &AggregationJob{
		sessionManager: sessionManager,
		hbaseReader:    hbaseReader,
		bqWriter:       bqWriter,
		analyzer:       analyzer,
		interval:       interval,
		ctx:            ctx,
		cancel:         cancel,
	}
}

// Start begins the aggregation job
func (aj *AggregationJob) Start() {
	go aj.run()
	go aj.processExpiredSessions()
}

// Stop gracefully stops the aggregation job
func (aj *AggregationJob) Stop() {
	aj.cancel()
}

// run executes the aggregation job on a schedule
func (aj *AggregationJob) run() {
	ticker := time.NewTicker(aj.interval)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			if err := aj.aggregateSessionData(); err != nil {
				fmt.Printf("Aggregation job error: %v\n", err)
			}

		case <-aj.ctx.Done():
			return
		}
	}
}

// aggregateSessionData aggregates data from all closed sessions
func (aj *AggregationJob) aggregateSessionData() error {
	aj.mu.Lock()
	defer aj.mu.Unlock()

	fmt.Println("Running session data aggregation...")

	// This would typically query closed sessions from the last interval
	// For now, we'll process based on expired session notifications

	return nil
}

// processExpiredSessions listens for expired sessions and creates summaries
func (aj *AggregationJob) processExpiredSessions() {
	expiredChannel := aj.sessionManager.GetExpiredSessionChannel()

	for {
		select {
		case sessionID := <-expiredChannel:
			go aj.createSessionSummary(sessionID)

		case <-aj.ctx.Done():
			return
		}
	}
}

// createSessionSummary creates and writes a session summary to BigQuery
func (aj *AggregationJob) createSessionSummary(sessionID string) error {
	// Get session from manager
	session, exists := aj.sessionManager.GetSession(sessionID)
	if !exists {
		return fmt.Errorf("session %s not found", sessionID)
	}

	// Get events from HBase
	events, err := aj.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return fmt.Errorf("failed to get session events: %w", err)
	}

	if len(events) == 0 {
		return nil
	}

	// Calculate duration
	var endTime time.Time
	if session.EndTime != nil {
		endTime = *session.EndTime
	} else {
		endTime = session.LastActiveTime
	}

	duration := endTime.Sub(session.StartTime)

	// Count action types
	actionCounts := make(map[EventType]int)
	uniqueScreens := make(map[string]bool)

	for _, event := range events {
		actionCounts[event.EventType]++
		if event.ScreenName != "" {
			uniqueScreens[event.ScreenName] = true
		}
	}

	// Detect anomalies
	anomalies, err := aj.analyzer.DetectAnomalies(sessionID)
	if err != nil {
		fmt.Printf("Failed to detect anomalies for session %s: %v\n", sessionID, err)
	}

	hasAnomaly := len(anomalies) > 0
	anomalyTypes := make([]string, 0)
	if hasAnomaly {
		for _, anomaly := range anomalies {
			anomalyTypes = append(anomalyTypes, anomaly.AnomalyType)
		}
	}

	// Create summary
	summary := SessionSummary{
		SessionID:     sessionID,
		UserID:        session.UserID,
		StartTime:     session.StartTime,
		EndTime:       endTime,
		Duration:      int64(duration.Seconds()),
		EventCount:    len(events),
		UniqueScreens: len(uniqueScreens),
		ActionCounts:  actionCounts,
		HasAnomaly:    hasAnomaly,
		AnomalyTypes:  anomalyTypes,
	}

	// Write to BigQuery
	if err := aj.bqWriter.WriteSummary(summary); err != nil {
		return fmt.Errorf("failed to write summary to BQ: %w", err)
	}

	fmt.Printf("Created summary for session %s: %d events, duration %d seconds, %d anomalies\n",
		sessionID, summary.EventCount, summary.Duration, len(anomalyTypes))

	return nil
}

// GetTopActionsGlobal returns the most used actions across all sessions in a time range
func (aj *AggregationJob) GetTopActionsGlobal(startTime, endTime time.Time) ([]ActionStats, error) {
	// This would typically query BigQuery for aggregated data
	// For demonstration, returning structure

	// Example query would be:
	// SELECT
	//   action_type,
	//   SUM(count) as total_count,
	//   COUNT(DISTINCT session_id) as session_count
	// FROM user_behavior_events
	// WHERE timestamp BETWEEN @start AND @end
	// GROUP BY action_type
	// ORDER BY total_count DESC

	return []ActionStats{}, nil
}

// GetAnomalyReport generates a report of all anomalies in a time range
func (aj *AggregationJob) GetAnomalyReport(startTime, endTime time.Time) ([]AnomalyDetection, error) {
	// This would query BigQuery for sessions with anomalies
	// Example query:
	// SELECT *
	// FROM session_summaries
	// WHERE has_anomaly = true
	//   AND start_time BETWEEN @start AND @end
	// ORDER BY start_time DESC

	return []AnomalyDetection{}, nil
}

// AnalyzeSessionBehavior provides a comprehensive analysis for a specific session
func (aj *AggregationJob) AnalyzeSessionBehavior(sessionID string) (*SessionAnalysisReport, error) {
	// Get session events
	events, err := aj.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return nil, err
	}

	// Get most used actions
	mostUsed, err := aj.analyzer.GetMostUsedActions(sessionID)
	if err != nil {
		return nil, err
	}

	// Get action patterns
	patterns, err := aj.analyzer.GetActionPatterns(sessionID, 3)
	if err != nil {
		return nil, err
	}

	// Detect anomalies
	anomalies, err := aj.analyzer.DetectAnomalies(sessionID)
	if err != nil {
		return nil, err
	}

	// Detect repeated patterns
	repeatedPatterns, err := aj.analyzer.DetectRepeatedPatterns(sessionID)
	if err != nil {
		return nil, err
	}

	return &SessionAnalysisReport{
		SessionID:        sessionID,
		EventCount:       len(events),
		MostUsedActions:  mostUsed,
		CommonPatterns:   patterns,
		Anomalies:        anomalies,
		RepeatedPatterns: repeatedPatterns,
	}, nil
}

// SessionAnalysisReport contains comprehensive analysis of a session
type SessionAnalysisReport struct {
	SessionID        string                  `json:"session_id"`
	EventCount       int                     `json:"event_count"`
	MostUsedActions  []ActionStats           `json:"most_used_actions"`
	CommonPatterns   []ActionPattern         `json:"common_patterns"`
	Anomalies        []AnomalyDetection      `json:"anomalies"`
	RepeatedPatterns []RepeatedActionPattern `json:"repeated_patterns"`
}
