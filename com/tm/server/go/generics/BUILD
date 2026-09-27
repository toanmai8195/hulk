load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "generics_lib",
    srcs = ["main.go"],
    importpath = "hulk/server/go/generics",
    visibility = ["//visibility:private"],
)

go_binary(
    name = "generics",
    embed = [":generics_lib"],
    visibility = ["//visibility:public"],
)
