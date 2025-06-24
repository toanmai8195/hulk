load("@rules_go//go:deps.bzl", "go_register_toolchains")

def _setup_go_toolchain_impl(ctx):
    go_register_toolchains(version = "1.24.1")

setup_go_toolchain = module_extension(implementation = _setup_go_toolchain_impl)