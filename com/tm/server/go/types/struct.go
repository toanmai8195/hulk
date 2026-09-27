package main

import "fmt"

type Vertex struct {
	X, Y int
}

func structDemo1() {
	v := Vertex{1, 2}
	fmt.Println(v.X)
	fmt.Println(v.Y)
	fmt.Println(v)
}

func structDemo2() {
	v := Vertex{3, 4}
	p := &v
	fmt.Println(v)
	p.X = 5
	fmt.Println(v)
}

func structDemo3() {
	v1 := Vertex{1, 2}
	v2 := Vertex{X: 5}
	v3 := Vertex{}
	p := &Vertex{3, 4}
	q := &v1
	fmt.Println(v1, v2, v3, p, q, p.X, q.X)
	q.X = 6
	fmt.Println(v1, v1.X, q, q.X)
	e := v1
	fmt.Println(v1, e)
	e.X = 7
	fmt.Println(v1, e, e.X)
}

func structDemo() {
	structDemo1()
	fmt.Println("=============")
	structDemo2()
	fmt.Println("=============")
	structDemo3()
}
