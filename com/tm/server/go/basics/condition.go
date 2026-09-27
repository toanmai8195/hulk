package main

import (
	"fmt"
	"runtime"
	"time"
)

func testSumLimit(a, b, limit int) int {
	if sum := a + b; sum < limit {
		return sum
	}

	return limit
}

func checkOS() {
	fmt.Print("Go runs on ")
	switch os := runtime.GOOS; os {
	case "darwin":
		fmt.Println("macOS.")
	case "linux":
		fmt.Println("Linux.")
	default:
		// freebsd, openbsd,
		// plan9, windows...
		fmt.Printf("%s.\n", os)
	}
}

func greeting() {
	t := time.Now()
	switch {
	case t.Hour() < 12:
		fmt.Println("Good morning!")
	case t.Hour() < 17:
		fmt.Println("Good afternoon.")
	default:
		fmt.Println("Good evening.")
	}
}

func stackingDefer() {
	fmt.Println("counting")

	for i := 0; i < 10; i++ {
		defer fmt.Println(i)
	}

	fmt.Println("done")
}

func conditionDemo() {
	defer fmt.Println("end")
	fmt.Println(testSumLimit(1, 4, 4))
	checkOS()
	greeting()
	stackingDefer()
}
