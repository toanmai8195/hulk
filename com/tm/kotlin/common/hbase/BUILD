load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "hbase_client",
    srcs = glob(["*.kt"]),
    deps = [
       "//tools/plugins:dagger",
       "@maven//:org_apache_hbase_hbase_client",
       "@maven//:org_apache_hbase_hbase_common",
       "@maven//:org_apache_hadoop_hadoop_common",
       "@maven//:io_vertx_vertx_core",
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "hbase_test",
    main_class = "com.tm.kotlin.common.hbase.HBaseTestKt",
    deps = [],
    runtime_deps = [":hbase_client"],
)
