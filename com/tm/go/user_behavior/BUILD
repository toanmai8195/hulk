load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "user_behavior_lib",
    srcs = [
        "models.go",
        "errors.go",
        "session_manager.go",
        "hbase_writer.go",
        "bigquery_writer.go",
        "behavior_analyzer.go",
        "aggregation_job.go",
        "event_collector.go",
        "main.go",
    ],
    importpath = "com/tm/go/user_behavior",
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_google_uuid//:go_default_library",
        "@com_github_tsuna_gohbase//:go_default_library",
        "@com_github_tsuna_gohbase//hrpc:go_default_library",
        "@com_google_cloud_go_bigquery//:go_default_library",
    ],
)

go_binary(
    name = "user_behavior",
    srcs = ["cmd/main.go"],
    deps = [
        ":user_behavior_lib",
    ],
)
