package user_behavior

import "errors"

var (
	ErrEventChannelFull = errors.New("event channel is full")
	ErrSessionNotFound  = errors.New("session not found")
	ErrHBaseWriteFailed = errors.New("hbase write failed")
	ErrBQWriteFailed    = errors.New("bigquery write failed")
)
