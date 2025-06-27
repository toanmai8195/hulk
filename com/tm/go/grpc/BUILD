load("//tools/rules:com_tm_container.bzl", "com_tm_go_image")

com_tm_go_image(
    name = "grpc_server",
    package_name = package_name(),
    srcs = [
        "grpc_main.go",
    ],
    exposedPorts = ["1995"],
    deps = [
        "//com/tm/go/lib/model/grpc:message_go_grpc",
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
        "@org_golang_google_grpc//:go_default_library",
    ],
)
