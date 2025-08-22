java_library(
    name = "grpc_test_lib",
    srcs = glob(["*.java"]),
    deps = [
        "//com/tm/java/service/grpc/proto:event_java_grpc",
        "@maven//:io_grpc_grpc_netty_shaded",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "grpc_server",
    main_class = "com.tm.java.service.grpc.service.GrpcTest",
    deps = [],
    runtime_deps = [":grpc_test_lib"],
)
