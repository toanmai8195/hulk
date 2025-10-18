load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "producer_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_apache_kafka_kafka_clients",
    ],
)

java_binary(
    name = "producer",
    main_class = "com.tm.kotlin.kafka_flink.producer.WordCountProducerKt",
    runtime_deps = [":producer_lib"],
)
