package api

import (
	"encoding/json"
	"net/http"
	"strings"

	"com.tm.go/es/client"
)

type Handler struct {
	ES *client.ESClient
}

func NewHandler(esClient *client.ESClient) *Handler {
	return &Handler{ES: esClient}
}

func (h *Handler) SaveMessage(w http.ResponseWriter, r *http.Request) {
	var body struct {
		MessageId string `json:"messageId"`
		RoomId    string `json:"roomId"`
		Content   string `json:"content"`
	}
	json.NewDecoder(r.Body).Decode(&body)
	err := h.ES.IndexMessage(body.MessageId, body.RoomId, body.Content)
	if err != nil {
		http.Error(w, err.Error(), 500)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]bool{"success": true})
}

func (h *Handler) SearchRoomIdByRegex(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Regex string `json:"regex"`
	}
	json.NewDecoder(r.Body).Decode(&body)
	matches, err := h.ES.SearchByRegex(strings.ToLower(body.Regex))
	if err != nil {
		http.Error(w, err.Error(), 500)
		return
	}
	roomIds := map[string]struct{}{}
	for _, match := range matches {
		roomIds[match["roomId"]] = struct{}{}
	}
	var result []string
	for roomId := range roomIds {
		result = append(result, roomId)
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"roomIds": result,
	})
}

func (h *Handler) SearchMessageIdByRegex(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Regex string `json:"regex"`
	}
	json.NewDecoder(r.Body).Decode(&body)
	matches, err := h.ES.SearchByRegex(strings.ToLower(body.Regex))
	if err != nil {
		http.Error(w, err.Error(), 500)
		return
	}
	var ids []string
	for _, match := range matches {
		ids = append(ids, match["messageId"])
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": ids,
	})
}
