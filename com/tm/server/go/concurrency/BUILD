load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "concurrency_lib",
    srcs = ["main.go"],
    importpath = "hulk/server/go/concurrency",
    visibility = ["//visibility:private"],
)

go_binary(
    name = "concurrency",
    embed = [":concurrency_lib"],
    visibility = ["//visibility:public"],
)
