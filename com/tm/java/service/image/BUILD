load("@rules_java//java:defs.bzl", "java_binary", "java_library")

java_binary(
    name = "image",
    srcs = glob(["*.java"]),
    main_class = "com.tm.java.service.image.ConvertBase64Main",
    runtime_deps = [],
    data = ["image.txt"]
)

exports_files(["image.txt"])
