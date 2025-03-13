load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "hello_lib",
    srcs = ["Main.kt"],
    deps = [
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
        ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "hello",
    main_class = "com.tm.MainKt",
    deps = [],
    runtime_deps = [":hello_lib"],
)