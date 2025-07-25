load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "grpc_server_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/models/grpc:message_java_grpc",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "grpc_server",
    main_class = "com.tm.kotlin.service.coroutine.grpc_server.GrpcServerKt",
    deps = [],
    runtime_deps = [":grpc_server_lib"],
)
