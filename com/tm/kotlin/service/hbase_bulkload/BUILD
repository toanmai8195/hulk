load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "tar")

##############################################
# Kotlin Library
##############################################
kt_jvm_library(
    name = "hbase_bulkload_lib",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//com/tm/kotlin/common/hbase:hbase_client",
        "//tools/plugins:dagger",

        # Vert.x + coroutines
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",

        # Bitmap
        "@maven//:org_roaringbitmap_RoaringBitmap",

        # Hadoop + HBase
        "@maven//:org_apache_hadoop_hadoop_common",
        "@maven//:org_apache_hadoop_hadoop_hdfs",
        "@maven//:org_apache_hbase_hbase_client",
        "@maven//:org_apache_hbase_hbase_common",
        "@maven//:org_apache_hbase_hbase_mapreduce",
        "@maven//:org_apache_hbase_hbase_server",
        "@maven//:org_apache_hadoop_hadoop_mapreduce_client_core",
        "@maven//:org_apache_hadoop_hadoop_mapreduce_client_jobclient",

        # MinIO + OkHttp
        "@maven//:io_minio_minio",
#         "@maven//:com_squareup_okhttp3_okhttp",
#         "@maven//:com_squareup_okio_okio",

        # Other deps
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib_jdk8",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib_common",
    ],
)

##############################################
# Java fat-jar (same model as counter_svc)
##############################################
java_binary(
    name = "hbase_bulkload",
    main_class = "com.tm.kotlin.service.hbase_bulkload.MainKt",
    runtime_deps = [
        ":hbase_bulkload_lib",

        # ---- FORCE HADOOP + HBASE RUNTIME ----
        "@maven//:org_apache_hadoop_hadoop_common",
        "@maven//:org_apache_hadoop_hadoop_hdfs",
        "@maven//:org_apache_hadoop_hadoop_hdfs_client",
        "@maven//:org_apache_hadoop_hadoop_mapreduce_client_core",
        "@maven//:org_apache_hadoop_hadoop_mapreduce_client_jobclient",

        "@maven//:org_apache_hbase_hbase_client",
        "@maven//:org_apache_hbase_hbase_common",
        "@maven//:org_apache_hbase_hbase_mapreduce",
        "@maven//:org_apache_hbase_hbase_server",

        # ---- MINIO ----
        "@maven//:io_minio_minio",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:com_squareup_okio_okio",
    ],
    visibility = ["//visibility:public"],
)

##############################################
# Pack the fat jar
##############################################
# Bazel automatically creates:
#   hbase_bulkload_deploy.jar
tar(
    name = "hbase_bulkload_tar",
    srcs = ["hbase_bulkload_deploy.jar"],
)

##############################################
# Docker image with Eclipse Temurin (has chmod)
##############################################
oci_image(
    name = "hbase_bulkload_image",
    base = "@eclipse_temurin",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/hbase_bulkload/hbase_bulkload_deploy.jar",
    ],
    tars = [":hbase_bulkload_tar"],
)

##############################################
# Load into docker
##############################################
oci_load(
    name = "hbase_bulkload_image_load",
    image = ":hbase_bulkload_image",
    repo_tags = ["com.tm.kotlin.hbase_bulkload:latest"],
)

# Alias for shorter command
alias(
    name = "image",
    actual = ":hbase_bulkload_image_load",
)