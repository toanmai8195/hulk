load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "rabbitmq_producers_lib",
    srcs = [
        "simple_queue.go",
        "competing_consumers.go",
        "priority_queue.go",
        "publish_subscribe.go",
        "rpc_pattern.go",
        "direct_exchange.go",
        "main.go",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_google_uuid//:go_default_library",
        "@com_github_rabbitmq_amqp091_go//:go_default_library",
    ],
)

go_binary(
    name = "rabbitmq_producers",
    embed = [":rabbitmq_producers_lib"],
)