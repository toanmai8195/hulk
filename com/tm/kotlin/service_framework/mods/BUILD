load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "http_route",
    srcs = glob(["*.kt"]),
    deps = [
       "//tools/plugins:dagger",
       "@maven//:io_vertx_vertx_core",
       "@maven//:io_vertx_vertx_web",
       "//com/tm/kotlin/common/http:factory"
    ],
    visibility = ["//visibility:public"],
)