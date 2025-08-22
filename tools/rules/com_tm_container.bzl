load("@rules_go//go:def.bzl", "go_binary", "go_library")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_load")
load("@tar.bzl", "mutate", "tar")

def com_tm_go_image(name, package_name, srcs, deps = [], importpath = "", exposedPorts = ["8080"], visibility = ["//visibility:public"]):
    """
    Builds a Go binary, packages it into a tarball, and creates an OCI image and docker load target.

    Args:
        name: The base name for all targets (binary, tar, image, docker load).
        srcs: List of Go source files for the library.
        deps: List of Go dependencies.
        importpath: Go import path for the library.
        exposedPorts: List of ports to expose in the container (as strings).
        visibility: Visibility of the generated targets.
        package_name: Package name of build file
    """

    lib_name = name + "_lib"
    local_bin_name = name
    docker_bin_name = name + "_docker_bin"
    tar_name = name + "_tar"
    image_name = name + "_image"
    docker_name = name + "_docker"

    go_library(
        name = lib_name,
        srcs = srcs,
        importpath = importpath,
        visibility = visibility,
        deps = deps,
    )

    go_binary(
        name = docker_bin_name,
        embed = [":" + lib_name],
        goos = "linux",
        goarch = "amd64",
    )

    go_binary(
        name = local_bin_name,
        embed = [":" + lib_name],
    )

    tar(
        name = tar_name,
        srcs = [":" + docker_bin_name],
        out = name + ".tar",
        mutate = mutate(
            strip_prefix = package_name + "/" + docker_bin_name + "_",
        ),
    )

    native.genrule(
        name = name + "_created",
        outs = [name + "_created.txt"],
        cmd = "bash -c 'echo -n $$(date -u +%Y-%m-%dT%H:%M:%SZ) > $@'",
    )

    oci_image(
        name = image_name,
        base = "@distroless_base",
        entrypoint = ["/" + docker_bin_name],
        tars = [":" + tar_name],
        exposed_ports = exposedPorts,
        created = ":" + name + "_created.txt",
    )

    oci_load(
        name = docker_name,
        image = ":" + image_name,
        repo_tags = ["com.tm.go.%s:v1.0.0" % name],
    )
