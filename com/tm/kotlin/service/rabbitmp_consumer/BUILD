load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "rabbitmp_consumer",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:com_rabbitmq_amqp_client",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_slf4j_slf4j_simple",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "consumer_main",
    main_class = "com.tm.kotlin.service.rabbitmp_consumer.ConsumerMainKt",
    runtime_deps = [":rabbitmp_consumer"],
)