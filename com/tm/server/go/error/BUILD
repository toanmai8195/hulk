load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "error_lib",
    srcs = ["main.go"],
    importpath = "hulk/server/go/error",
    visibility = ["//visibility:private"],
)

go_binary(
    name = "error",
    embed = [":error_lib"],
    visibility = ["//visibility:public"],
)
