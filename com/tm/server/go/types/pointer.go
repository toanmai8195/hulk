package main

import "fmt"

func pointerDemo() {
	i, j := 42, 2701

	p := &i
	fmt.Println(p)
	fmt.Println(*p)
	fmt.Println("-----------")

	*p = 21
	fmt.Println(p)
	fmt.Println(*p)
	fmt.Println(i)
	fmt.Println(&i)
	fmt.Println(*&i)
	fmt.Println("-----------")

	p = &j
	*p = *p / 37
	fmt.Println(p)
	fmt.Println(*p)
	fmt.Println(i)
	fmt.Println(&i)
	fmt.Println(j)
	fmt.Println(&j)
	fmt.Println(*&j)
}
