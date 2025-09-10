load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

kt_jvm_library(
    name = "friend_svc_lib",
    srcs = glob([
        "verticles/friendbybitmap/*.kt",
        "verticles/friendbycell/*.kt",
        "*.kt"
        ]),
    deps = [
        "//tools/plugins:dagger",
        "@maven//:io_vertx_vertx_core",
        "@maven//:org_roaringbitmap_RoaringBitmap",
        "//com/tm/kotlin/common/hbase:hbase_client",
         "//com/tm/kotlin/common/mods/base",
         "//com/tm/kotlin/common/utils",
         "//com/tm/kotlin/common/mods/monitor",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "friend_svc",
    main_class = "com.tm.kotlin.service.friend.FriendMainKt",
    runtime_deps = [":friend_svc_lib"]
)

tar(
    name = "friend_tar",
    srcs = ["friend_svc_deploy.jar"]
)

oci_image(
    name = "friend_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/friend/friend_svc_deploy.jar",
    ],
    tars = [":friend_tar"],
)

oci_load(
    name = "friend_image_load",
    image = ":friend_image",
    repo_tags = ["com.tm.java.friend_service:v1.0.2"],
)