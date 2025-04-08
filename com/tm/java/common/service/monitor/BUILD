load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "monitor",
    srcs = glob(["*.java"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "//com/tm/java/common/annotation",
        "@maven//:io_vertx_vertx_web",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "//tools/plugins:dagger",
    ],
    visibility = ["//visibility:public"],
)
