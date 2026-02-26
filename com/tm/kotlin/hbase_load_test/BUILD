load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "tar")

##############################################
# Kotlin Library
##############################################
kt_jvm_library(
    name = "hbase_load_test_lib",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//com/tm/kotlin/common/hbase:hbase_client",
        "//com/tm/kotlin/common/mods/base:base",
        "//com/tm/kotlin/common/mods/monitor:monitor",
        "//tools/plugins:dagger",

        # Vert.x
        "@maven//:io_vertx_vertx_core",

        # Metrics
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:io_micrometer_micrometer_registry_prometheus",

        # Hadoop + HBase
        "@maven//:org_apache_hadoop_hadoop_common",
        "@maven//:org_apache_hbase_hbase_client",
        "@maven//:org_apache_hbase_hbase_common",

        # Kotlin stdlib
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib_jdk8",
    ],
)

##############################################
# Java fat-jar
##############################################
java_binary(
    name = "hbase_load_test",
    main_class = "com.tm.kotlin.hbase_load_test.HBaseLoadTestMainKt",
    runtime_deps = [
        ":hbase_load_test_lib",

        # HBase runtime
        "@maven//:org_apache_hadoop_hadoop_common",
        "@maven//:org_apache_hbase_hbase_client",
        "@maven//:org_apache_hbase_hbase_common",
    ],
    visibility = ["//visibility:public"],
)

##############################################
# Pack the fat jar
##############################################
tar(
    name = "hbase_load_test_tar",
    srcs = ["hbase_load_test_deploy.jar"],
)

##############################################
# Docker image with Eclipse Temurin
##############################################
oci_image(
    name = "hbase_load_test_image",
    base = "@eclipse_temurin",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/hbase_load_test/hbase_load_test_deploy.jar",
    ],
    tars = [":hbase_load_test_tar"],
)

##############################################
# Load into docker
##############################################
oci_load(
    name = "hbase_load_test_image_load",
    image = ":hbase_load_test_image",
    repo_tags = ["com.tm.kotlin.hbase_load_test:latest"],
)

# Alias for shorter command
alias(
    name = "image",
    actual = ":hbase_load_test_image_load",
)
