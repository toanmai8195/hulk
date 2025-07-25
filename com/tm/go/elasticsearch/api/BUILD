load("@rules_go//go:def.bzl","go_library")

go_library(
    name = "api",
    srcs = [
        "handler.go",
    ],
    visibility = ["//visibility:public"],
    importpath = "com.tm.go/es/api",
    deps = [
        "//com/tm/go/elasticsearch/client"
    ],
)