load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "verticles",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "//tools/plugins:dagger",
        "//com/tm/kotlin/service/coroutine/httpserver/handler",
        "@maven//:io_vertx_vertx_web",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "//com/tm/kotlin/common/http/tracking",
    ],
    visibility = ["//visibility:public"],
)
