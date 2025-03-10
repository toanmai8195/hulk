load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "go_ws_lib",
    srcs = [
        "main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_gorilla_websocket//:go_default_library",
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
        "@com_github_shirou_gopsutil//:go_default_library",
    ],
)

go_binary(
    name = "go_ws",
    embed = [":go_ws_lib"],
    deps = [],
)