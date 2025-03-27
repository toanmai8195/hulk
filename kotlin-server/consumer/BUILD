load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "consumer_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
        "@maven//:org_apache_kafka_kafka_clients",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "consumer",
    main_class = "com.tm.consumer.MainKt",
    deps = [],
    runtime_deps = [":consumer_lib"],
)