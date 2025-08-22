# MoMo HTTP Client

This package provides an HTTP client for calling MoMo APIs, specifically the helios-social-relationships mutual friends load endpoint.

## Features

- HTTP client with configurable timeout (30 seconds default)
- Proper error handling and response parsing
- Support for custom request parameters
- Convenience method with default values
- JSON request/response handling

## Usage

### Basic Setup

```go
package main

import (
    "log"
    "github.com/your-org/momo/httpclient"
)

func main() {
    // Create client
    client := httpclient.NewClient(
        "https://s.dev.mservice.io",  // baseURL
        "dev",                        // environment
        "your_bearer_token_here"     // authorization token
    )
    
    // Use the client...
}
```

### Call API with Custom Parameters

```go
req := httpclient.MutualLoadRequest{
    ActorID:   "01660000999",
    PartnerID: "01660000990",
    Offset:    0,
    Limit:     20,
}

resp, err := client.LoadMutualFriends(req)
if err != nil {
    log.Printf("Error: %v", err)
    return
}

fmt.Printf("Response: %+v\n", resp)
```

### Call API with Default Parameters

```go
resp, err := client.LoadMutualFriendsWithDefaults("01660000999", "01660000990")
if err != nil {
    log.Printf("Error: %v", err)
    return
}
```

## API Endpoint

The client calls the following endpoint:
- **URL**: `https://s.dev.mservice.io/internal//helios-social-relationships/v1/mutual/load`
- **Method**: POST
- **Headers**:
  - `env`: Environment (e.g., "dev")
  - `Authorization`: Bearer token
  - `Content-Type`: application/json

## Request Structure

```json
{
    "actor_id": "01660000999",
    "partner_id": "01660000990",
    "offset": 0,
    "limit": 20
}
```

## Response Structure

```json
{
    "code": 200,
    "message": "success",
    "data": {}
}
```

## Error Handling

The client handles various error scenarios:
- Request marshaling errors
- HTTP request creation errors
- Network errors
- Non-200 HTTP status codes
- Response parsing errors

## Configuration

- **Timeout**: 30 seconds (configurable in the client)
- **Base URL**: Configurable via constructor
- **Environment**: Configurable via constructor
- **Token**: Configurable via constructor

## Example

See `example.go` for a complete working example of how to use the client.
