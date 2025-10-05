package user_behavior

import (
	"context"
	"sync"
	"time"

	"github.com/google/uuid"
)

const (
	// SessionTimeout defines when a session is considered inactive
	SessionTimeout = 30 * time.Minute

	// SessionCleanupInterval defines how often to check for expired sessions
	SessionCleanupInterval = 5 * time.Minute
)

// SessionManager manages user sessions and handles session lifecycle
type SessionManager struct {
	sessions      map[string]*Session
	mu            sync.RWMutex
	eventChannel  chan UserEvent
	sessionExpiry chan string
	ctx           context.Context
	cancel        context.CancelFunc
}

// NewSessionManager creates a new session manager
func NewSessionManager(eventBufferSize int) *SessionManager {
	ctx, cancel := context.WithCancel(context.Background())

	sm := &SessionManager{
		sessions:      make(map[string]*Session),
		eventChannel:  make(chan UserEvent, eventBufferSize),
		sessionExpiry: make(chan string, 100),
		ctx:           ctx,
		cancel:        cancel,
	}

	return sm
}

// Start begins the session manager background workers
func (sm *SessionManager) Start(numWorkers int) {
	// Start event processing workers
	for i := 0; i < numWorkers; i++ {
		go sm.eventWorker()
	}

	// Start session timeout monitor
	go sm.sessionTimeoutMonitor()

	// Start cleanup worker
	go sm.cleanupWorker()
}

// Stop gracefully stops the session manager
func (sm *SessionManager) Stop() {
	sm.cancel()
	close(sm.eventChannel)
}

// CreateSession creates a new session for a user
func (sm *SessionManager) CreateSession(userID string) string {
	sessionID := uuid.New().String()

	sm.mu.Lock()
	defer sm.mu.Unlock()

	session := &Session{
		SessionID:      sessionID,
		UserID:         userID,
		StartTime:      time.Now(),
		LastActiveTime: time.Now(),
		IsActive:       true,
		EventCount:     0,
		Events:         make([]UserEvent, 0),
	}

	sm.sessions[sessionID] = session

	return sessionID
}

// TrackEvent adds an event to the event channel for processing
func (sm *SessionManager) TrackEvent(event UserEvent) error {
	select {
	case sm.eventChannel <- event:
		return nil
	case <-sm.ctx.Done():
		return sm.ctx.Err()
	default:
		// Channel is full, log or handle overflow
		return ErrEventChannelFull
	}
}

// GetSession retrieves a session by ID
func (sm *SessionManager) GetSession(sessionID string) (*Session, bool) {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	session, exists := sm.sessions[sessionID]
	return session, exists
}

// GetUserSessions returns all active sessions for a user
func (sm *SessionManager) GetUserSessions(userID string) []*Session {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	var userSessions []*Session
	for _, session := range sm.sessions {
		if session.UserID == userID && session.IsActive {
			userSessions = append(userSessions, session)
		}
	}

	return userSessions
}

// CloseSession explicitly closes a session
func (sm *SessionManager) CloseSession(sessionID string) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	if session, exists := sm.sessions[sessionID]; exists {
		now := time.Now()
		session.EndTime = &now
		session.IsActive = false
	}
}

// eventWorker processes events from the event channel
func (sm *SessionManager) eventWorker() {
	for {
		select {
		case event, ok := <-sm.eventChannel:
			if !ok {
				return
			}

			sm.processEvent(event)

		case <-sm.ctx.Done():
			return
		}
	}
}

// processEvent processes a single event
func (sm *SessionManager) processEvent(event UserEvent) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	session, exists := sm.sessions[event.SessionID]
	if !exists {
		// Auto-create session if it doesn't exist
		session = &Session{
			SessionID:      event.SessionID,
			UserID:         event.UserID,
			StartTime:      event.Timestamp,
			LastActiveTime: event.Timestamp,
			IsActive:       true,
			EventCount:     0,
			Events:         make([]UserEvent, 0),
		}
		sm.sessions[event.SessionID] = session
	}

	// Update session
	session.LastActiveTime = event.Timestamp
	session.EventCount++

	// Store event in session (limited buffer to prevent memory overflow)
	if len(session.Events) < 1000 {
		session.Events = append(session.Events, event)
	}

	// Handle session close event
	if event.EventType == EventAppClose {
		now := time.Now()
		session.EndTime = &now
		session.IsActive = false
	}
}

// sessionTimeoutMonitor monitors sessions for timeouts
func (sm *SessionManager) sessionTimeoutMonitor() {
	ticker := time.NewTicker(SessionCleanupInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			sm.checkSessionTimeouts()

		case <-sm.ctx.Done():
			return
		}
	}
}

// checkSessionTimeouts identifies and marks timed-out sessions
func (sm *SessionManager) checkSessionTimeouts() {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	now := time.Now()

	for sessionID, session := range sm.sessions {
		if !session.IsActive {
			continue
		}

		// Check if session has been inactive for too long
		if now.Sub(session.LastActiveTime) > SessionTimeout {
			session.EndTime = &session.LastActiveTime
			session.IsActive = false

			// Notify about expired session
			select {
			case sm.sessionExpiry <- sessionID:
			default:
				// Channel full, skip notification
			}
		}
	}
}

// cleanupWorker removes old inactive sessions from memory
func (sm *SessionManager) cleanupWorker() {
	ticker := time.NewTicker(10 * time.Minute)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			sm.cleanupOldSessions()

		case <-sm.ctx.Done():
			return
		}
	}
}

// cleanupOldSessions removes sessions older than 24 hours
func (sm *SessionManager) cleanupOldSessions() {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	now := time.Now()
	cutoff := now.Add(-24 * time.Hour)

	for sessionID, session := range sm.sessions {
		if !session.IsActive && session.EndTime != nil && session.EndTime.Before(cutoff) {
			delete(sm.sessions, sessionID)
		}
	}
}

// GetExpiredSessionChannel returns the channel that emits expired session IDs
func (sm *SessionManager) GetExpiredSessionChannel() <-chan string {
	return sm.sessionExpiry
}
