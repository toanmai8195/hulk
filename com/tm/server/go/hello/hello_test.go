package hello

import "testing"

func TestGreet(t *testing.T) {
	if got := Greet("hulk"); got != "Hello, hulk" {
		t.Fatalf("Greet() = %q", got)
	}
}
