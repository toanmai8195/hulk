package main

import (
	"time"
)

type UserProfileAccessEvent struct {
	EventID       string    `json:"event_id"`
	Timestamp     time.Time `json:"timestamp"`
	UserID        string    `json:"user_id"`
	AccessorID    string    `json:"accessor_id"`
	AccessorType  string    `json:"accessor_type"`
	APIEndpoint   string    `json:"api_endpoint"`
	HTTPMethod    string    `json:"http_method"`
	RequestIP     string    `json:"request_ip"`
	UserAgent     string    `json:"user_agent"`
	ResponseCode  int       `json:"response_code"`
	ResponseTime  int64     `json:"response_time_ms"`
	DataRequested string    `json:"data_requested"`
	DataReturned  string    `json:"data_returned"`
	ErrorMessage  string    `json:"error_message,omitempty"`
	IsBlocked     bool      `json:"is_blocked"`
	BlockReason   string    `json:"block_reason,omitempty"`
	SessionID     string    `json:"session_id,omitempty"`
	RequestSize   int64     `json:"request_size_bytes"`
	ResponseSize  int64     `json:"response_size_bytes"`
}

type EventMetadata struct {
	ProducerID    string    `json:"producer_id"`
	ProducedAt    time.Time `json:"produced_at"`
	Version       string    `json:"version"`
	Environment   string    `json:"environment"`
}

type KafkaMessage struct {
	Event    UserProfileAccessEvent `json:"event"`
	Metadata EventMetadata          `json:"metadata"`
}