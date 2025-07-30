load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "handler",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "//tools/plugins:dagger",
        "//com/tm/kotlin/common/error",
        "//com/tm/kotlin/service/coroutine/httpserver/hbase",
    ],
    visibility = ["//visibility:public"],
)
