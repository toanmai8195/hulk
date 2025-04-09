load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "monitor",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/mods/base",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
         "//com/tm/kotlin/common/http:factory"
    ],
    visibility = ["//visibility:public"],
)