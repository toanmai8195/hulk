load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "basics_lib",
    srcs = [
        "condition.go",
        "functions.go",
        "main.go",
    ],
    importpath = "hulk/server/go/basics",
    visibility = ["//visibility:private"],
)

go_binary(
    name = "basics",
    embed = [":basics_lib"],
    visibility = ["//visibility:public"],
)
