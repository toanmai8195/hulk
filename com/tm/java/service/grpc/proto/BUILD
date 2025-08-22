load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc_java//:defs.bzl", "java_proto_library", "java_grpc_library")

proto_library(
    name = "event_proto",
    srcs = ["event.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "event_java_proto",
    protos = [":event_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "event_java_grpc",
    protos = [":event_proto"],
    visibility = ["//visibility:public"],
)
