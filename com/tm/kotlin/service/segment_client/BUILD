load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "segment_client_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:io_vertx_vertx_web_client",
        "@maven//:io_vertx_vertx_core",
        "//tools/plugins:dagger",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "segment_client",
    main_class = "com.tm.kotlin.service.segment_client.SegmentApiMainKt",
    runtime_deps = [":segment_client_lib"],
)
