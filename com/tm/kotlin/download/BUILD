load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "download_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "download",
    main_class = "com.tm.kotlin.download.DownloadMainKt",
    deps = [],
    runtime_deps = [":download_lib"],
)
