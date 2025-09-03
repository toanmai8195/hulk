load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_library(
    name = "openapi_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//com/tm/kotlin/common/mods/base",
        "//com/tm/kotlin/common/mods/monitor",
        "@maven//:io_vertx_vertx_lang_kotlin_coroutines",
        "@maven//:io_vertx_vertx_web_openapi",
        "//com/tm/kotlin/service/openapi/handlers",
        "//com/tm/kotlin/common/http/tracking",
        "@maven//:io_swagger_parser_v3_swagger_parser",
        "@maven//:org_webjars_swagger_ui",
        "@maven//:io_swagger_core_v3_swagger_annotations",
        "@maven//:io_swagger_core_v3_swagger_integration",
        "@maven//:io_swagger_core_v3_swagger_core",
        "@maven//:io_swagger_core_v3_swagger_models",
    ],
    resources = ["openapi.yaml"],
#     resource_strip_prefix = "com/tm/kotlin/service/openapi",
    visibility = ["//visibility:public"],
)

kt_jvm_binary(
    name = "openapi",
    main_class = "com.tm.kotlin.service.openapi.OpenApiMainKt",
    deps = [],
    runtime_deps = [":openapi_lib"],
    data = [":openapi.yaml"]
)

exports_files(["openapi.yaml"])
