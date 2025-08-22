load("//tools/rules:com_tm_container.bzl", "com_tm_go_image")

com_tm_go_image(
    name = "checkfriends",
    package_name = package_name(),
    srcs = ["checkfriends.go"],
    exposedPorts = ["8080"],
    deps = [
        "@com_github_prometheus_client_golang//prometheus",
        "@com_github_prometheus_client_golang//prometheus/promhttp",
    ]
)
