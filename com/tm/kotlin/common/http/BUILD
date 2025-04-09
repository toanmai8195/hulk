load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "factory",
    srcs = glob(["*.kt"]),
    deps = [
       "//tools/plugins:dagger",
       "@maven//:io_vertx_vertx_core",
       "@maven//:io_vertx_vertx_web",
    ],
    visibility = ["//visibility:public"],
)