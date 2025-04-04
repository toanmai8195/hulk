package(default_visibility = ["//visibility:public"])

java_library(
    name = "dagger",
    exports = [
        "@maven//:com_google_dagger_dagger",
        "@maven//:javax_inject_javax_inject",
    ],
    exported_plugins = [
        ":dagger_plugin",
    ],
)

java_plugin(
    name = "dagger_plugin",
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    deps = [
        "@maven//:com_google_dagger_dagger_compiler",
        "@maven//:com_google_dagger_dagger_producers",
    ],
)

java_library(
    name = "autovalue",
    exported_plugins = [":autovalue_plugin"],
    exports = [
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:com_google_code_gson_gson",
        "@maven//:javax_annotation_javax_annotation_api",
    ],
)

java_plugin(
    name = "autovalue_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [
        "@maven//:com_google_auto_auto_common",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:javax_annotation_javax_annotation_api",
    ],
)

