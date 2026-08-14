# tonic-grpc

Rust gRPC server on `tonic`, the native Rust gRPC stack built on `hyper`/`tower`/`prost`. Serves cleartext gRPC always, and additionally serves TLS-terminated gRPC on a second port when certificates are present.

## Stack

- **Language:** Rust (`rust:1-bookworm`)
- **Framework:** tonic 0.13 (`tls-ring`, `transport`), prost 0.13
- **TLS:** tonic's built-in `ServerTlsConfig` (ring), identity loaded from PEM
- **Build:** Multi-stage, protoc via `protobuf-compiler`, `debian:bookworm-slim` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Returns `SumReply { result: a + b }` from `SumRequest {a, b}` |

## Notes

- At startup, checks for `/certs/server.crt` and `/certs/server.key`; if both exist it runs two servers concurrently via `tokio::spawn` + `try_join!` — plaintext h2c on `:8080` and TLS on `:8443` — otherwise it serves only plaintext h2c on `:8080`
- Proto types generated at build time via `tonic_build::compile_protos` in `build.rs`
- No custom `[profile.release]` (no LTO / `target-cpu` tuning) — plain `cargo build --release`
