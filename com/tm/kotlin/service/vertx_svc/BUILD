load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "vertx_svc_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor",
        "//com/tm/kotlin/service/vertx_svc/mods:http_route",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "vertx_svc",
    main_class = "com.tm.kotlin.service.vertx_svc.AppKt",
    deps = [],
    runtime_deps = [":vertx_svc_lib"],
)
