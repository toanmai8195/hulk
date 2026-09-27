package main

import "fmt"

func add(x, y int) int {
	return x + y
}

func swap(x, y string) (string, string) {
	return y, x
}

func split(sum int) (x, y int) {
	x = sum * 4 / 9
	y = sum - x
	return
}

func functionsDemo() {
	var i11, i12 int = 1, 2
	fmt.Printf("Results func add(x,y int)=%d\n", add(i11, i12))

	var i21, i22 string = "hello", "golang"
	rs21, rs22 := swap(i21, i22)
	fmt.Printf("Results func swap(x,y string)=(%s, %s) \n", rs21, rs22)

	var i3 = 17
	rs31, rs32 := split(i3)
	fmt.Printf("Results func split(sum int)=(%d, %d)\n", rs31, rs32)
}
