load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc_java//:defs.bzl", "java_proto_library", "java_grpc_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

# Proto definitions
proto_library(
    name = "compute_proto",
    srcs = ["compute.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "compute_java_proto",
    protos = [":compute_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "compute_java_grpc",
    protos = [":compute_proto"],
    visibility = ["//visibility:public"],
)

# Service implementation
kt_jvm_library(
    name = "grpc_compute_service_lib",
    srcs = ["ComputeService.kt"],
    deps = [
        ":compute_java_grpc",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:io_micrometer_micrometer_core",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "grpc_compute_service",
    main_class = "com.tm.kotlin.service.grpc.netty.ComputeServiceKt",
    runtime_deps = [":grpc_compute_service_lib"],
    jvm_flags = [
        "-Djava.util.logging.config.file=com/tm/kotlin/service/grpc/netty/logging.properties",
    ],
    resources = ["logging.properties"],
)

# Load test client
kt_jvm_library(
    name = "load_test_client_lib",
    srcs = ["LoadTestClient.kt"],
    deps = [
        ":compute_java_grpc",
        "@maven//:io_grpc_grpc_netty_shaded",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:io_micrometer_micrometer_core",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "load_test_client",
    main_class = "com.tm.kotlin.service.grpc.netty.LoadTestClientKt",
    runtime_deps = [":load_test_client_lib"],
)

# Docker image packaging
tar(
    name = "grpc_compute_tar",
    srcs = [":grpc_compute_service_deploy.jar"]
)

oci_image(
    name = "grpc_compute_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/kotlin/service/grpc/netty/grpc_compute_service_deploy.jar",
    ],
    tars = [":grpc_compute_tar"],
)

oci_load(
    name = "grpc_compute_image_load",
    image = ":grpc_compute_image",
    repo_tags = ["com.tm.kotlin.grpc_compute_service:latest"],
)
