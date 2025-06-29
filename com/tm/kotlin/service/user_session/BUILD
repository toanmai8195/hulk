load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "user_session_lib",
    srcs = glob(["*.kt"]),
    deps = [],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "user_session",
    main_class = "com.tm.kotlin.service.user_session.UserSessionMainKt",
    deps = [],
    runtime_deps = [":user_session_lib"],
)
