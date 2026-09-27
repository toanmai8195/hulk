# hulk

Repo thử nghiệm cá nhân.

```
com/tm/
├── server/   # Bazel module (Go, Kotlin/Java, Python toolchains)
│   ├── MODULE.bazel
│   ├── go/   # Thử nghiệm Go
│   └── tools/rules/  # Macro build image (rules_oci), plugin Dagger
└── app/      # Thử nghiệm ReactJS
```

## Server (Bazel)

Chạy mọi lệnh Bazel trong thư mục `com/tm/server`:

```sh
cd com/tm/server
bazel test //go/...        # chạy toàn bộ test Go
bazel build //go/...
bazel run //:gazelle       # sinh/cập nhật BUILD cho Go
bazel mod tidy             # dọn use_repo trong MODULE.bazel
```

Thêm dependency Go: sửa `com/tm/server/go.mod` (`go get ...`) rồi chạy `bazel mod tidy`.

## App (ReactJS)

Code React đặt trong `com/tm/app`.
