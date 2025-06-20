load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "bigquery_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:com_google_cloud_google_cloud_bigquery",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_api_gax_grpc",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_opencsv_opencsv",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "bigquery",
    main_class = "com.tm.kotlin.gcp.bigquery.MainKt",
    deps = [],
    runtime_deps = [":bigquery_lib"],
    data = ["//com/tm/kotlin/gcp/iam:hulk-auth.json","//com/tm/kotlin/gcp/iam:client-auth.json"]
)
