load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library","kt_jvm_binary")

kt_jvm_library(
    name = "utils",
    srcs = glob(["*.kt"]),
    deps = [
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "utils_test",
    main_class = "com.tm.kotlin.common.utils.PhoneUtilsKt",
    deps = [],
    runtime_deps = [":utils"],
)