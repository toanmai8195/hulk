package client

import (
	"bytes"
	"context"
	"encoding/json"
	"github.com/elastic/go-elasticsearch/v8"
	"github.com/elastic/go-elasticsearch/v8/esapi"
)

type ESClient struct {
	Client *elasticsearch.Client
}

func NewESClient() (*ESClient, error) {
	cfg := elasticsearch.Config{}
	client, err := elasticsearch.NewClient(cfg)
	if err != nil {
		return nil, err
	}
	return &ESClient{Client: client}, nil
}

func (es *ESClient) IndexMessage(messageId, roomId, content string) error {
	doc := map[string]string{
		"messageId": messageId,
		"roomId":    roomId,
		"content":   content,
	}
	data, _ := json.Marshal(doc)
	req := esapi.IndexRequest{
		Index:      "messages",
		DocumentID: messageId,
		Body:       bytes.NewReader(data),
		Refresh:    "true",
	}
	res, err := req.Do(context.Background(), es.Client)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	return nil
}

func (es *ESClient) SearchByRegex(regex string) ([]map[string]string, error) {
	query := map[string]interface{}{
		"query": map[string]interface{}{
			"regexp": map[string]interface{}{
				"content": regex,
			},
		},
	}
	data, _ := json.Marshal(query)
	res, err := es.Client.Search(
		es.Client.Search.WithIndex("messages"),
		es.Client.Search.WithBody(bytes.NewReader(data)),
	)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	var r map[string]interface{}
	json.NewDecoder(res.Body).Decode(&r)
	hits := r["hits"].(map[string]interface{})["hits"].([]interface{})
	var result []map[string]string
	for _, hit := range hits {
		source := hit.(map[string]interface{})["_source"].(map[string]interface{})
		result = append(result, map[string]string{
			"roomId":    source["roomId"].(string),
			"messageId": source["messageId"].(string),
		})
	}
	return result, nil
}
