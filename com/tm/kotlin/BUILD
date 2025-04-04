load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "hash_lib",
    srcs = glob(["*.kt"]),
    deps = [],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "hash",
    main_class = "com.tm.kotlin.MainKt",
    deps = [],
    runtime_deps = [":hash_lib"],
)
