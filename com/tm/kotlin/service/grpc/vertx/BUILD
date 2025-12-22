load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc_java//:defs.bzl", "java_proto_library", "java_grpc_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

# Proto definitions
proto_library(
    name = "vertx_compute_proto",
    srcs = ["compute.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "vertx_compute_java_proto",
    protos = [":vertx_compute_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "vertx_compute_java_grpc",
    protos = [":vertx_compute_proto"],
    visibility = ["//visibility:public"],
)

# Vert.x Service implementation
kt_jvm_library(
    name = "vertx_grpc_compute_service_lib",
    srcs = ["VertxComputeService.kt"],
    deps = [
        ":vertx_compute_java_grpc",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_grpc",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:io_micrometer_micrometer_core",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "vertx_grpc_compute_service",
    main_class = "com.tm.kotlin.service.grpc.vertx.VertxComputeServiceKt",
    runtime_deps = [":vertx_grpc_compute_service_lib"],
    jvm_flags = [
        "-Djava.util.logging.config.file=com/tm/kotlin/service/grpc/vertx/logging.properties",
    ],
    resources = ["logging.properties"],
)

# Vert.x Load test client
kt_jvm_library(
    name = "vertx_load_test_client_lib",
    srcs = ["VertxLoadTestClient.kt"],
    deps = [
        ":vertx_compute_java_grpc",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:io_micrometer_micrometer_core",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "vertx_load_test_client",
    main_class = "com.tm.kotlin.service.grpc.vertx.VertxLoadTestClientKt",
    runtime_deps = [":vertx_load_test_client_lib"],
)

# Docker image packaging
tar(
    name = "vertx_grpc_compute_tar",
    srcs = [":vertx_grpc_compute_service_deploy.jar"]
)

oci_image(
    name = "vertx_grpc_compute_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/grpc/vertx/vertx_grpc_compute_service_deploy.jar",
    ],
    tars = [":vertx_grpc_compute_tar"],
)

oci_load(
    name = "vertx_grpc_compute_image_load",
    image = ":vertx_grpc_compute_image",
    repo_tags = ["com.tm.kotlin.vertx_grpc_compute_service:latest"],
)
