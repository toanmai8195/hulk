load("@rules_go//go:def.bzl", "go_binary","go_library")

go_library(
    name = "mock_evn_lib",
    srcs = [
        "evn_consumer.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_confluentinc_confluent_kafka_go//kafka:go_default_library",
    ],
)

go_binary(
    name = "mock_evn",
    embed = [":mock_evn_lib"],
)