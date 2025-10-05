package user_behavior

import (
	"context"
	"fmt"
	"sort"
	"sync"
	"time"
)

// BehaviorAnalyzer analyzes user behavior patterns and detects anomalies
type BehaviorAnalyzer struct {
	sessionManager *SessionManager
	hbaseReader    *HBaseWriter
	ctx            context.Context
	cancel         context.CancelFunc
	mu             sync.RWMutex
}

// NewBehaviorAnalyzer creates a new behavior analyzer
func NewBehaviorAnalyzer(sessionManager *SessionManager, hbaseReader *HBaseWriter) *BehaviorAnalyzer {
	ctx, cancel := context.WithCancel(context.Background())

	return &BehaviorAnalyzer{
		sessionManager: sessionManager,
		hbaseReader:    hbaseReader,
		ctx:            ctx,
		cancel:         cancel,
	}
}

// Start begins the behavior analyzer
func (ba *BehaviorAnalyzer) Start() {
	go ba.monitorExpiredSessions()
}

// Stop gracefully stops the analyzer
func (ba *BehaviorAnalyzer) Stop() {
	ba.cancel()
}

// GetMostUsedActions returns the most frequently used actions
func (ba *BehaviorAnalyzer) GetMostUsedActions(sessionID string) ([]ActionStats, error) {
	events, err := ba.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return nil, err
	}

	if len(events) == 0 {
		return []ActionStats{}, nil
	}

	// Count action types
	actionCounts := make(map[EventType]int64)
	for _, event := range events {
		actionCounts[event.EventType]++
	}

	// Convert to stats
	var stats []ActionStats
	totalEvents := int64(len(events))

	for eventType, count := range actionCounts {
		stats = append(stats, ActionStats{
			EventType:  eventType,
			Count:      count,
			Percentage: float64(count) / float64(totalEvents) * 100,
		})
	}

	// Sort by count descending
	sort.Slice(stats, func(i, j int) bool {
		return stats[i].Count > stats[j].Count
	})

	return stats, nil
}

// GetActionPatterns identifies common sequences of actions
func (ba *BehaviorAnalyzer) GetActionPatterns(sessionID string, patternLength int) ([]ActionPattern, error) {
	events, err := ba.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return nil, err
	}

	if len(events) < patternLength {
		return []ActionPattern{}, nil
	}

	// Extract patterns
	patternCounts := make(map[string]*ActionPattern)

	for i := 0; i <= len(events)-patternLength; i++ {
		pattern := make([]EventType, patternLength)
		startTime := events[i].Timestamp
		endTime := events[i+patternLength-1].Timestamp

		for j := 0; j < patternLength; j++ {
			pattern[j] = events[i+j].EventType
		}

		// Create pattern key
		key := patternKey(pattern)

		if existing, exists := patternCounts[key]; exists {
			existing.Count++
			duration := endTime.Sub(startTime).Seconds()
			existing.AvgDuration = (existing.AvgDuration*float64(existing.Count-1) + duration) / float64(existing.Count)
		} else {
			patternCounts[key] = &ActionPattern{
				Pattern:     pattern,
				Count:       1,
				AvgDuration: endTime.Sub(startTime).Seconds(),
			}
		}
	}

	// Convert to slice and sort
	var patterns []ActionPattern
	for _, p := range patternCounts {
		if p.Count > 1 { // Only include patterns that occur more than once
			patterns = append(patterns, *p)
		}
	}

	sort.Slice(patterns, func(i, j int) bool {
		return patterns[i].Count > patterns[j].Count
	})

	return patterns, nil
}

// DetectAnomalies identifies unusual behavior patterns
func (ba *BehaviorAnalyzer) DetectAnomalies(sessionID string) ([]AnomalyDetection, error) {
	events, err := ba.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return nil, err
	}

	var anomalies []AnomalyDetection

	// Check for repeated actions
	repeatedAnomalies := ba.detectRepeatedActions(events)
	anomalies = append(anomalies, repeatedAnomalies...)

	// Check for rapid fire events
	rapidFireAnomalies := ba.detectRapidFireEvents(events)
	anomalies = append(anomalies, rapidFireAnomalies...)

	// Check for stuck patterns (e.g., typing->send->back loop)
	stuckPatternAnomalies := ba.detectStuckPatterns(events)
	anomalies = append(anomalies, stuckPatternAnomalies...)

	// Check for session without close
	if len(events) > 0 {
		lastEvent := events[len(events)-1]
		if lastEvent.EventType != EventAppClose && time.Since(lastEvent.Timestamp) > SessionTimeout {
			anomalies = append(anomalies, AnomalyDetection{
				SessionID:   sessionID,
				UserID:      lastEvent.UserID,
				AnomalyType: "missing_session_close",
				Description: "Session ended without explicit close event",
				DetectedAt:  time.Now(),
				Severity:    "low",
			})
		}
	}

	return anomalies, nil
}

// detectRepeatedActions finds actions repeated excessively
func (ba *BehaviorAnalyzer) detectRepeatedActions(events []UserEvent) []AnomalyDetection {
	var anomalies []AnomalyDetection

	if len(events) < 5 {
		return anomalies
	}

	// Sliding window to detect same action repeated
	windowSize := 5
	for i := 0; i <= len(events)-windowSize; i++ {
		firstType := events[i].EventType
		allSame := true

		for j := 1; j < windowSize; j++ {
			if events[i+j].EventType != firstType {
				allSame = false
				break
			}
		}

		if allSame {
			sequence := make([]EventType, windowSize)
			for j := 0; j < windowSize; j++ {
				sequence[j] = events[i+j].EventType
			}

			anomalies = append(anomalies, AnomalyDetection{
				SessionID:     events[i].SessionID,
				UserID:        events[i].UserID,
				AnomalyType:   "repeated_action",
				Description:   fmt.Sprintf("Action '%s' repeated %d times in a row", firstType, windowSize),
				EventSequence: sequence,
				DetectedAt:    time.Now(),
				Severity:      "medium",
			})

			i += windowSize - 1 // Skip ahead
		}
	}

	return anomalies
}

// detectRapidFireEvents finds events happening too quickly
func (ba *BehaviorAnalyzer) detectRapidFireEvents(events []UserEvent) []AnomalyDetection {
	var anomalies []AnomalyDetection

	rapidFireThreshold := 100 * time.Millisecond
	rapidFireCount := 0

	for i := 1; i < len(events); i++ {
		timeDiff := events[i].Timestamp.Sub(events[i-1].Timestamp)

		if timeDiff < rapidFireThreshold {
			rapidFireCount++
		} else {
			rapidFireCount = 0
		}

		if rapidFireCount >= 5 {
			anomalies = append(anomalies, AnomalyDetection{
				SessionID:   events[i].SessionID,
				UserID:      events[i].UserID,
				AnomalyType: "rapid_fire_events",
				Description: fmt.Sprintf("Multiple events fired within %v", rapidFireThreshold),
				DetectedAt:  time.Now(),
				Severity:    "high",
			})
			rapidFireCount = 0
		}
	}

	return anomalies
}

// detectStuckPatterns finds users stuck in repetitive patterns (typing->send->back)
func (ba *BehaviorAnalyzer) detectStuckPatterns(events []UserEvent) []AnomalyDetection {
	var anomalies []AnomalyDetection

	// Define problematic patterns
	stuckPattern := []EventType{EventTyping, EventSendMessage, EventBackToHome}

	// Count pattern occurrences
	patternCount := 0
	patternIndex := 0

	for i := 0; i < len(events); i++ {
		if events[i].EventType == stuckPattern[patternIndex] {
			patternIndex++

			if patternIndex == len(stuckPattern) {
				patternCount++
				patternIndex = 0

				// If pattern repeats 3+ times, it's anomalous
				if patternCount >= 3 {
					anomalies = append(anomalies, AnomalyDetection{
						SessionID:     events[i].SessionID,
						UserID:        events[i].UserID,
						AnomalyType:   "stuck_pattern",
						Description:   fmt.Sprintf("User stuck in typing->send->back pattern (%d times)", patternCount),
						EventSequence: stuckPattern,
						DetectedAt:    time.Now(),
						Severity:      "high",
					})
					patternCount = 0
				}
			}
		} else {
			patternIndex = 0
			patternCount = 0
		}
	}

	return anomalies
}

// DetectRepeatedPatterns specifically detects repeated action sequences for chat scenarios
func (ba *BehaviorAnalyzer) DetectRepeatedPatterns(sessionID string) ([]RepeatedActionPattern, error) {
	events, err := ba.hbaseReader.GetSessionEvents(sessionID)
	if err != nil {
		return nil, err
	}

	var patterns []RepeatedActionPattern

	// Look for typing -> send -> back pattern
	chatPattern := []EventType{EventTyping, EventSendMessage, EventBackToHome}
	chatRepeats := ba.countPatternRepeats(events, chatPattern)

	if chatRepeats.RepeatCount >= 3 {
		patterns = append(patterns, chatRepeats)
	}

	return patterns, nil
}

// countPatternRepeats counts consecutive occurrences of a pattern
func (ba *BehaviorAnalyzer) countPatternRepeats(events []UserEvent, pattern []EventType) RepeatedActionPattern {
	result := RepeatedActionPattern{
		Pattern:     pattern,
		RepeatCount: 0,
	}

	if len(events) == 0 {
		return result
	}

	result.UserID = events[0].UserID
	result.SessionID = events[0].SessionID

	consecutiveMatches := 0
	patternIdx := 0

	for i := 0; i < len(events); i++ {
		if events[i].EventType == pattern[patternIdx] {
			if patternIdx == 0 && result.FirstOccurrence.IsZero() {
				result.FirstOccurrence = events[i].Timestamp
			}

			patternIdx++

			if patternIdx == len(pattern) {
				consecutiveMatches++
				result.LastOccurrence = events[i].Timestamp
				patternIdx = 0
			}
		} else {
			if consecutiveMatches > result.RepeatCount {
				result.RepeatCount = consecutiveMatches
			}
			consecutiveMatches = 0
			patternIdx = 0
		}
	}

	if consecutiveMatches > result.RepeatCount {
		result.RepeatCount = consecutiveMatches
	}

	return result
}

// monitorExpiredSessions monitors for expired sessions and triggers analysis
func (ba *BehaviorAnalyzer) monitorExpiredSessions() {
	expiredChannel := ba.sessionManager.GetExpiredSessionChannel()

	for {
		select {
		case sessionID := <-expiredChannel:
			// Perform analysis on expired session
			go ba.analyzeExpiredSession(sessionID)

		case <-ba.ctx.Done():
			return
		}
	}
}

// analyzeExpiredSession performs full analysis on an expired session
func (ba *BehaviorAnalyzer) analyzeExpiredSession(sessionID string) {
	// Detect anomalies
	anomalies, err := ba.DetectAnomalies(sessionID)
	if err != nil {
		fmt.Printf("Error detecting anomalies for session %s: %v\n", sessionID, err)
		return
	}

	if len(anomalies) > 0 {
		fmt.Printf("Detected %d anomalies in session %s\n", len(anomalies), sessionID)
		for _, anomaly := range anomalies {
			fmt.Printf("  - %s: %s (severity: %s)\n", anomaly.AnomalyType, anomaly.Description, anomaly.Severity)
		}
	}
}

// patternKey creates a string key from a pattern
func patternKey(pattern []EventType) string {
	key := ""
	for i, et := range pattern {
		if i > 0 {
			key += "->"
		}
		key += string(et)
	}
	return key
}
