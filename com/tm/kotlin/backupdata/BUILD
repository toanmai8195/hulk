load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "backupdata_lib",
    srcs = glob(["*.kt",]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:com_google_dagger_dagger",
        "@maven//:javax_inject_javax_inject",
        "//com/tm/kotlin/common/hbase:hbase_client",
        "//com/tm/kotlin/common/cassandra:cassandra_client",
        "//com/tm/kotlin/common/error:error",
        "//tools/plugins:dagger",
    ],
)

java_binary(
    name = "backupdata",
    main_class = "com.tm.kotlin.backupdata.BackupDataMainKt",
    runtime_deps = [":backupdata_lib"],
)

tar(
    name = "backupdata_tar",
    srcs = ["backupdata_deploy.jar"]
)

oci_image(
    name = "backupdata_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/backupdata/backupdata_deploy.jar",
    ],
    tars = [":backupdata_tar"],
)

oci_load(
    name = "backupdata_image_load",
    image = ":backupdata_image",
    repo_tags = ["com.tm.kotlin.backupdata:v1.0.0"],
)