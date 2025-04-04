load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "consumer_lib",
    srcs = glob(["*.java"]),
    deps = [
        "//plugins:dagger",
        "//plugins:autovalue",
        "@maven//:io_vertx_vertx_core",
        "@maven//:io_vertx_vertx_web",
        "//com/tm/java/common/module:module",
        "//com/tm/java/common/service/deployment",
        "//com/tm/java/common/annotation",
    ],
)

java_binary(
    name = "consumer",
    main_class = "com.tm.java.consumer.Main",
    runtime_deps = [
        ":consumer_lib"
        ],
)
