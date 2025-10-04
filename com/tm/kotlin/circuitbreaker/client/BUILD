load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "circuitbreaker_client_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_web",
        "@maven//:io_vertx_vertx_web_client",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:io_github_resilience4j_resilience4j_circuitbreaker",
        "@maven//:io_github_resilience4j_resilience4j_kotlin",
        "//tools/plugins:dagger",
        "//com/tm/kotlin/common/mods/base",
    ],
)

java_binary(
    name = "client",
    main_class = "com.tm.kotlin.circuitbreaker.client.CircuitBreakerClientKt",
    runtime_deps = [":circuitbreaker_client_lib"],
)
