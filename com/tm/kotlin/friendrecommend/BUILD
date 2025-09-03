load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "friendrecommend_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor", 
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:io_vertx_vertx_web",
        "//com/tm/kotlin/common/http/tracking",
        "//com/tm/kotlin/common/error",
        "//tools/plugins:dagger",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "friendrecommend",
    main_class = "com.tm.kotlin.friendrecommend.FriendRecommendMainKt",
    deps = [],
    runtime_deps = [":friendrecommend_lib"],
)