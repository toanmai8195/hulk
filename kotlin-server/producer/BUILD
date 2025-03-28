load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "producer_lib",
    srcs = ["Producer.kt"],
    deps = [
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
        "@maven//:org_apache_kafka_kafka_clients",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "producer",
    main_class = "com.tm.producer.ProducerKt",
    deps = [],
    runtime_deps = [":producer_lib"],
)