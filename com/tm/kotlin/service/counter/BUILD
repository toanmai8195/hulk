load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "counter_svc_lib",
    srcs = glob([
        "verticles/*.kt",
        "*.kt"
        ]),
    deps = [
        "//tools/plugins:dagger",
        "@maven//:io_vertx_vertx_core",
        "@maven//:org_roaringbitmap_RoaringBitmap",
        "//com/tm/kotlin/common/hbase:hbase_client",
         "//com/tm/kotlin/common/mods/base",
         "//com/tm/kotlin/common/utils",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "counter_svc",
    main_class = "com.tm.kotlin.service.counter.CounterMainKt",
    deps = [],
    runtime_deps = [":counter_svc_lib"],
)
