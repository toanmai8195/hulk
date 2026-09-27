load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "types_lib",
    srcs = [
        "interface.go",
        "main.go",
        "pointer.go",
        "struct.go",
    ],
    importpath = "hulk/server/go/types",
    visibility = ["//visibility:private"],
)

go_binary(
    name = "types",
    embed = [":types_lib"],
    visibility = ["//visibility:public"],
)
