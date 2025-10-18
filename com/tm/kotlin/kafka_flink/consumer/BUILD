load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "consumer_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_apache_flink_flink_streaming_java",
        "@maven//:org_apache_flink_flink_clients",
        "@maven//:org_apache_flink_flink_connector_kafka",
        "@maven//:org_apache_flink_flink_connector_base",
        "@maven//:org_apache_kafka_kafka_clients",
    ],
)

java_binary(
    name = "consumer",
    main_class = "com.tm.kotlin.kafka_flink.consumer.WordCountConsumerKt",
    runtime_deps = [":consumer_lib"],
    jvm_flags = [
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
    ],
)
