load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

java_binary(
    name = "image",
    srcs = glob(["*.java"]),
    main_class = "com.tm.java.service.image.ConvertBase64Main",
    runtime_deps = [],
    data = ["image.txt"],
)

tar(
    name = "image_tar",
    srcs = ["image_deploy.jar"],
)

oci_image(
    name = "image_image",
    base = "@distroless_java",
    entrypoint = [
        "java",
        "-jar",
        "/com/tm/java/service/image/image_deploy.jar",
    ],
    tars = [":image_tar"],
)

oci_load(
    name = "image_image_load",
    image = ":image_image",
    repo_tags = ["com.tm.java.image_service:v1.0.1"],
)

exports_files(["image.txt"])
