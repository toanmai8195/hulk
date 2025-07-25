load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "generatedata_lib",
    srcs = [
        "generate.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
    ],
)

go_binary(
    name = "generatedata",
    embed = [":generatedata_lib"],
)