load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "ws_server_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "ws_server",
    main_class = "com.tm.kotlin.service.ws.WsServerKt",
    deps = [],
    runtime_deps = [":ws_server_lib"],
)
