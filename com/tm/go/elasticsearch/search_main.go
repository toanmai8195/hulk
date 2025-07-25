package main

import (
	"com.tm.go/es/api"
	"com.tm.go/es/client"
	"log"
	"net/http"
)

func main() {
	esClient, err := client.NewESClient()
	if err != nil {
		log.Fatalf("ES error: %v", err)
	}
	handler := api.NewHandler(esClient)

	http.HandleFunc("/api/message", handler.SaveMessage)
	http.HandleFunc("/api/search/rooms", handler.SearchRoomIdByRegex)
	http.HandleFunc("/api/search/messages", handler.SearchMessageIdByRegex)

	log.Println("Server running on :1998")
	log.Fatal(http.ListenAndServe(":1998", nil))
}
