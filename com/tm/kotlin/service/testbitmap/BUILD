load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "testbitmap_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:org_roaringbitmap_RoaringBitmap",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "testbitmap",
    main_class = "com.tm.kotlin.service.testbitmap.RoaringBitmapTestKt",
    runtime_deps = [":testbitmap_lib"],
)
