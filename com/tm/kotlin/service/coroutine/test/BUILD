load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "test_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

kt_jvm_binary(
    name = "test",
    main_class = "com.tm.kotlin.service.coroutine.test.MainKt",
    deps = [],
    runtime_deps = [":test_lib"],
)
