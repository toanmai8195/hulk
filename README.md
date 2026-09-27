# hulk

Personal playground repo.

```
com/tm/
├── server/   # Bazel module (Go, Kotlin/Java, Python toolchains)
│   ├── MODULE.bazel
│   ├── go/   # Go experiments
│   └── tools/rules/  # Image build macros (rules_oci), Dagger plugin
└── app/      # ReactJS experiments
```

## Server (Bazel)

Run all Bazel commands from `com/tm/server`:

```sh
cd com/tm/server
bazel test //go/...        # run all Go tests
bazel build //go/...
bazel run //:gazelle       # generate/update Go BUILD files
bazel mod tidy             # clean up use_repo in MODULE.bazel
```

To add a Go dependency: update `com/tm/server/go.mod` (`go get ...`), then run `bazel mod tidy`.

## App (ReactJS)

React code lives in `com/tm/app`.
