load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "server_lib",
    srcs = glob(["*.java"]),
    deps = [
           "@maven//:io_vertx_vertx_core",
           "@maven//:io_vertx_vertx_web",
        ],
)

java_binary(
    name = "server",
    main_class = "com.tm.JavaServerVertical",
    runtime_deps = [":server_lib"],
)