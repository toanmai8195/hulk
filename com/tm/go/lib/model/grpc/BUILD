load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_go//proto:def.bzl", "go_proto_library","go_grpc_library")

proto_library(
    name = "message_proto",
    srcs = ["message.proto"],
    visibility = ["//visibility:public"],
)

go_proto_library(
    name = "message_go_proto",
    importpath = "com.tm.go/model/grpc/message",
    protos = [":message_proto"],
    visibility = ["//visibility:public"],
)

go_grpc_library(
    name = "message_go_grpc",
    importpath = "com.tm.go/model/grpc/message",
    protos = [":message_proto"],
    visibility = ["//visibility:public"],
)