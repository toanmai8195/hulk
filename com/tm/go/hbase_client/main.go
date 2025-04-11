// package main
//
// import (
// 	"context"
// 	"fmt"
//
// 	"github.com/tsuna/gohbase"
// 	"github.com/tsuna/gohbase/hrpc"
// )
//
// func main() {
// 	// Connect tới HBase (qua native protocol)
// 	client := gohbase.NewClient("localhost")
//
// 	// Tạo request GET
// 	getRequest, err := hrpc.NewGetStr(context.Background(), "my_table", "my_row_key")
// 	if err != nil {
// 		panic(err)
// 	}
//
// 	// Gửi request tới HBase
// 	result, err := client.Get(getRequest)
// 	if err != nil {
// 		panic(err)
// 	}
//
// 	for _, cell := range result.Cells {
// 		fmt.Printf("Column: %s, Value: %s\n", cell.Qualifier, cell.Value)
// 	}
// }
