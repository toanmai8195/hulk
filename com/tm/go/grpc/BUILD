load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "grpc_lib",
    srcs = [
        "grpc_main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
        "//com/tm/go/lib/model/grpc:message_go_grpc",
        "@org_golang_google_grpc//:go_default_library",
    ],
)

go_binary(
    name = "grpc",
    embed = [":grpc_lib"],
)