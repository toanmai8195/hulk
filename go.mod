module hulk

go 1.24.1

require (
	com.tm.go/lib/model/ws_model v0.0.0-00010101000000-000000000000
	com.tm.go/model/grpc/message v0.0.0-00010101000000-000000000000
	github.com/IBM/sarama v1.46.1
	github.com/google/uuid v1.6.0
	github.com/gorilla/websocket v1.5.3
	github.com/prometheus/client_golang v1.21.1
	github.com/rabbitmq/amqp091-go v1.10.0
	google.golang.org/grpc v1.71.0
)

require (
	github.com/davecgh/go-spew v1.1.1 // indirect
	github.com/eapache/go-resiliency v1.7.0 // indirect
	github.com/eapache/go-xerial-snappy v0.0.0-20230731223053-c322873962e3 // indirect
	github.com/eapache/queue v1.1.0 // indirect
	github.com/golang/snappy v0.0.4 // indirect
	github.com/hashicorp/go-uuid v1.0.3 // indirect
	github.com/jcmturner/aescts/v2 v2.0.0 // indirect
	github.com/jcmturner/dnsutils/v2 v2.0.0 // indirect
	github.com/jcmturner/gofork v1.7.6 // indirect
	github.com/jcmturner/gokrb5/v8 v8.4.4 // indirect
	github.com/jcmturner/rpc/v2 v2.0.3 // indirect
	github.com/pierrec/lz4/v4 v4.1.22 // indirect
	github.com/rcrowley/go-metrics v0.0.0-20250401214520-65e299d6c5c9 // indirect
	golang.org/x/crypto v0.42.0 // indirect
)

require (
	github.com/beorn7/perks v1.0.1 // indirect
	github.com/cespare/xxhash/v2 v2.3.0 // indirect
	github.com/confluentinc/confluent-kafka-go v1.9.2
	github.com/grpc-ecosystem/go-grpc-prometheus v1.2.0 // indirect
	github.com/klauspost/compress v1.18.0 // indirect
	github.com/munnerz/goautoneg v0.0.0-20191010083416-a7dc8b61c822 // indirect
	github.com/prometheus/client_model v0.6.1 // indirect
	github.com/prometheus/common v0.62.0 // indirect
	github.com/prometheus/procfs v0.15.1 // indirect
	golang.org/x/net v0.44.0 // indirect
	golang.org/x/sys v0.36.0 // indirect
	golang.org/x/text v0.29.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20250324211829-b45e905df463 // indirect
	google.golang.org/protobuf v1.36.6 // indirect
)

replace google.golang.org/grpc => github.com/grpc/grpc-go v1.73.0

replace com.tm.go/es/api => ./com/tm/go/elasticsearch

replace com.tm.go/es/client => ./com/tm/go/elasticsearch

replace com.tm.go/model/grpc/message => ./com/tm/go/grpc

replace com.tm.go/lib/model/ws_model => ./com/tm/go/lib/model/ws
