load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "circuitbreaker_server_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_web",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "//tools/plugins:dagger",
        "//com/tm/kotlin/common/mods/base",
    ],
)

java_binary(
    name = "server",
    main_class = "com.tm.kotlin.circuitbreaker.server.CircuitBreakerServerKt",
    runtime_deps = [":circuitbreaker_server_lib"],
)
