load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "tracking",
    srcs = glob(["*.kt"]),
    deps = [
       "@maven//:io_micrometer_micrometer_registry_prometheus",
       "//tools/plugins:dagger",
    ],
    visibility = ["//visibility:public"],
)