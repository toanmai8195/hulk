load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "handler",
    srcs = glob(["*.kt"]),
    deps = [
       "@maven//:io_vertx_vertx_web",
    ],
    visibility = ["//visibility:public"],
)