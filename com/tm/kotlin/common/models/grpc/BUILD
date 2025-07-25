load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc_java//:defs.bzl", "java_proto_library", "java_grpc_library")

proto_library(
    name = "message_proto",
    srcs = ["message.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "message_java_proto",
    protos = [":message_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "message_java_grpc",
    protos = [":message_proto"],
    visibility = ["//visibility:public"],
)
