load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "server_lib",
    srcs = [
        "main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_gorilla_websocket//:go_default_library",
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
    ],
)

go_binary(
    name = "server",
    embed = [":serverlo_lib"],
    deps = [],
)