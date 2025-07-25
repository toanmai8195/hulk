load("@rules_go//go:def.bzl","go_library")

go_library(
    name = "client",
    srcs = [
        "client.go",
    ],
    visibility = ["//visibility:public"],
    importpath = "com.tm.go/es/client",
    deps = [
        "@com_github_elastic_go_elasticsearch_v8//:go_default_library",
        "@com_github_elastic_go_elasticsearch_v8//esapi:go_default_library",
    ],
)