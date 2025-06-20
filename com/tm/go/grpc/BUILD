load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "grpc_lib",
    srcs = [
        "main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
    ],
)

go_binary(
    name = "grpc",
    embed = [":grpc_lib"],
)