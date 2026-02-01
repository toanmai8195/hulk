load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "bitmap_loadtest_lib",
    srcs = glob(
        [
            "**/*.kt",
        ],
    ),
    deps = [
        "//tools/plugins:dagger",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:org_roaringbitmap_RoaringBitmap",
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "bitmap_loadtest_svc",
    main_class = "com.tm.kotlin.service.bitmap_loadtest.MainKt",
    runtime_deps = [":bitmap_loadtest_lib"],
    data = ["//com/tm/kotlin/service/bitmap_loadtest/data:segment50M.bin"],
)

tar(
    name = "bitmap_loadtest_tar",
    srcs = [
        "bitmap_loadtest_svc_deploy.jar",
        "//com/tm/kotlin/service/bitmap_loadtest/data:segment50M.bin",
    ],
)

oci_image(
    name = "bitmap_loadtest_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/bitmap_loadtest/bitmap_loadtest_svc_deploy.jar",
    ],
    tars = [":bitmap_loadtest_tar"],
)

oci_load(
    name = "bitmap_loadtest_image_load",
    image = ":bitmap_loadtest_image",
    repo_tags = ["com.tm.java.bitmap_loadtest:v1.0.0"],
)
