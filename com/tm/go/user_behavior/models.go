package user_behavior

import (
	"time"
)

// EventType defines the type of user action
type EventType string

const (
	EventAppOpen      EventType = "app_open"
	EventAppClose     EventType = "app_close"
	EventScreenView   EventType = "screen_view"
	EventButtonClick  EventType = "button_click"
	EventTyping       EventType = "typing"
	EventSendMessage  EventType = "send_message"
	EventBackToHome   EventType = "back_to_home"
	EventScrollStart  EventType = "scroll_start"
	EventScrollEnd    EventType = "scroll_end"
	EventSearch       EventType = "search"
	EventCustom       EventType = "custom"
)

// UserEvent represents a single user behavior event
type UserEvent struct {
	EventID    string                 `json:"event_id"`
	UserID     string                 `json:"user_id"`
	SessionID  string                 `json:"session_id"`
	EventType  EventType              `json:"event_type"`
	Timestamp  time.Time              `json:"timestamp"`
	ScreenName string                 `json:"screen_name,omitempty"`
	Metadata   map[string]interface{} `json:"metadata,omitempty"`
}

// Session represents a user session
type Session struct {
	SessionID      string      `json:"session_id"`
	UserID         string      `json:"user_id"`
	StartTime      time.Time   `json:"start_time"`
	LastActiveTime time.Time   `json:"last_active_time"`
	EndTime        *time.Time  `json:"end_time,omitempty"`
	EventCount     int         `json:"event_count"`
	IsActive       bool        `json:"is_active"`
	Events         []UserEvent `json:"-"` // Not serialized to save memory
}

// ActionPattern represents a sequence of actions
type ActionPattern struct {
	Pattern   []EventType `json:"pattern"`
	Count     int         `json:"count"`
	AvgDuration float64   `json:"avg_duration_seconds"`
}

// ActionStats represents statistics for a specific action type
type ActionStats struct {
	EventType EventType `json:"event_type"`
	Count     int64     `json:"count"`
	Percentage float64  `json:"percentage"`
}

// AnomalyDetection represents detected anomalies in user behavior
type AnomalyDetection struct {
	SessionID     string    `json:"session_id"`
	UserID        string    `json:"user_id"`
	AnomalyType   string    `json:"anomaly_type"`
	Description   string    `json:"description"`
	EventSequence []EventType `json:"event_sequence"`
	DetectedAt    time.Time `json:"detected_at"`
	Severity      string    `json:"severity"` // low, medium, high
}

// RepeatedActionPattern detects repeated actions (e.g., typing->send->back repeatedly)
type RepeatedActionPattern struct {
	UserID        string      `json:"user_id"`
	SessionID     string      `json:"session_id"`
	Pattern       []EventType `json:"pattern"`
	RepeatCount   int         `json:"repeat_count"`
	FirstOccurrence time.Time `json:"first_occurrence"`
	LastOccurrence  time.Time `json:"last_occurrence"`
}

// SessionSummary for BigQuery aggregation
type SessionSummary struct {
	SessionID      string            `json:"session_id"`
	UserID         string            `json:"user_id"`
	StartTime      time.Time         `json:"start_time"`
	EndTime        time.Time         `json:"end_time"`
	Duration       int64             `json:"duration_seconds"`
	EventCount     int               `json:"event_count"`
	UniqueScreens  int               `json:"unique_screens"`
	ActionCounts   map[EventType]int `json:"action_counts"`
	HasAnomaly     bool              `json:"has_anomaly"`
	AnomalyTypes   []string          `json:"anomaly_types"`
}
