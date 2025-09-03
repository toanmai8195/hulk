load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "handlers",
    srcs = glob(["*.kt"]),
    deps = [
        "//tools/plugins:dagger",
        "//com/tm/kotlin/common/error",
        "//com/tm/kotlin/common/http/handler",
        "@maven//:io_swagger_core_v3_swagger_annotations",
    ],
    visibility = ["//visibility:public"],
)
