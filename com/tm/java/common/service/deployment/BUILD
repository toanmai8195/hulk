load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "deployment",
    srcs = glob(["*.java"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "//tools/plugins:dagger",
    ],
    visibility = ["//visibility:public"],
)