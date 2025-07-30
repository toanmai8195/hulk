load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "hbase",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:org_apache_hbase_hbase_client",
        "@maven//:org_apache_hbase_hbase_common",
        "@maven//:org_apache_hadoop_hadoop_common",
        "//tools/plugins:dagger",
        "//com/tm/kotlin/common/error",
        "@maven//:org_roaringbitmap_RoaringBitmap",
    ],
    visibility = ["//visibility:public"],
)
