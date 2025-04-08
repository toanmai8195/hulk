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
    generates_api = 1,
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    deps = [
        "@maven//:com_google_dagger_dagger_compiler",
        "@maven//:com_google_dagger_dagger_producers",
    ],
)