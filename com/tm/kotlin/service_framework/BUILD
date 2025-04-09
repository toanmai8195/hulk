load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "service_framework_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor",
        "//com/tm/kotlin/service_framework/mods:http_route",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "service_framework",
    main_class = "com.tm.kotlin.service_framework.AppKt",
    deps = [],
    runtime_deps = [":service_framework_lib"],
)
