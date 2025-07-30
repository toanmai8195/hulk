load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "httpserver_lib",
    srcs = glob(["*.kt",]),
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor",
        "//com/tm/kotlin/service/coroutine/httpserver/verticles",
    ],
)

java_binary(
    name = "httpserver",
    main_class = "com.tm.kotlin.service.coroutine.httpserver.HttpServerKt",
    runtime_deps = [":httpserver_lib"],
)

tar(
    name = "httpserver_tar",
    srcs = ["httpserver_deploy.jar"]
)

oci_image(
    name = "httpserver_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/coroutine/httpserver/httpserver_deploy.jar",
    ],
    tars = [":httpserver_tar"],
)

oci_load(
    name = "httpserver_image_load",
    image = ":httpserver_image",
    repo_tags = ["com.tm.java.httpserver_coroutine:v1.0.0"],
)
