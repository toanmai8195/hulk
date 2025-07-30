load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "error",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:io_vertx_vertx_core"
        ],
    visibility = ["//visibility:public"],
)
