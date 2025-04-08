load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "module",
    srcs = glob(["*.java"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "//tools/plugins:dagger",
        "//com/tm/java/common/service/monitor",
        "//com/tm/java/common/service/deployment",
    ],
    visibility = ["//visibility:public"],
)
