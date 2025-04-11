load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "cassandra_client",
    srcs = glob(["*.kt"]),
    deps = [
       "//tools/plugins:dagger",
       "@maven//:com_datastax_oss_java_driver_core"
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "cassandra_test",
    main_class = "com.tm.kotlin.cassandra.CassandraTestKt",
    deps = [],
    runtime_deps = [":cassandra_client"],
)
