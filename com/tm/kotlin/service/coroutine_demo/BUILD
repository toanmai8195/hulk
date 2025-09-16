load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "coroutine_demo",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "demo_main",
    main_class = "com.tm.kotlin.service.coroutine_demo.DemoMainKt",
    runtime_deps = [":coroutine_demo"],
)
