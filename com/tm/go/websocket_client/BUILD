load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "websocket_client_lib",
    srcs = [
        "websocket_client_main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_gorilla_websocket//:go_default_library",
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
        "//com/tm/go/lib/model/ws"
    ],
)

go_binary(
    name = "websocket_client",
    embed = [":websocket_client_lib"],
)