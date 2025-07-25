package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"math/rand"
	"net/http"
	"time"
)

const (
	apiURL     = "http://localhost:1998/api/message"
	totalRooms = 100
	totalMsgs  = 1000
)

var sampleWords = []string{
	"hello", "world", "test", "chat", "message", "random", "golang", "elastic", "room", "fun", "number",
}

func randomContent() string {
	n := rand.Intn(5) + 3
	var words []string
	for i := 0; i < n; i++ {
		words = append(words, sampleWords[rand.Intn(len(sampleWords))])
	}
	return fmt.Sprintf("%s %d", joinWords(words), rand.Intn(1000))
}

func joinWords(words []string) string {
	return fmt.Sprintf("%s", bytes.Join([][]byte{[]byte(words[0]), []byte(words[1]), []byte(words[2])}, []byte(" ")))
}

func main() {
	rand.Seed(time.Now().UnixNano())

	for i := 1; i <= totalMsgs; i++ {
		msg := map[string]interface{}{
			"messageId": fmt.Sprintf("m%d", i),
			"roomId":    fmt.Sprintf("r%d", rand.Intn(totalRooms)+1),
			"content":   randomContent(),
		}

		data, _ := json.Marshal(msg)

		resp, err := http.Post(apiURL, "application/json", bytes.NewBuffer(data))
		if err != nil {
			fmt.Printf("Error sending message %d: %v\n", i, err)
			continue
		}
		resp.Body.Close()
		if resp.StatusCode != http.StatusOK {
			fmt.Printf("Failed for message %d: status %d\n", i, resp.StatusCode)
		} else {
			fmt.Printf("Sent message %d\n", i)
		}

		time.Sleep(10 * time.Millisecond) // nhỏ delay để không spam quá mạnh
	}
}
