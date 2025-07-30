load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "http_lib",
    srcs = glob(["*.kt",]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/service/coroutine/httpclient/verticles",
    ],
)

java_binary(
    name = "httpclient",
    main_class = "com.tm.kotlin.service.coroutine.httpclient.HttpClientKt",
    runtime_deps = [":http_lib"],
)

tar(
    name = "httpclient_tar",
    srcs = ["httpclient_deploy.jar"]
)

oci_image(
    name = "httpclient_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/coroutine/httpclient/httpclient_deploy.jar",
    ],
    tars = [":httpclient_tar"],
)

oci_load(
    name = "httpclient_image_load",
    image = ":httpclient_image",
    repo_tags = ["com.tm.java.httpclient_coroutine:v1.0.0"],
)
