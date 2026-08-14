# wtx-grpc

gRPC server built on `wtx`'s `Http2ServerFramework` with the `grpc-server` feature, serving unary gRPC over HTTP/2 cleartext using the `QuickProtobuf` codec.

## Stack

- **Language:** Rust nightly-2026-06-27 (via `rustup default`, base image `rust:1.95`)
- **Framework:** wtx 0.49 (`http2-server-framework` + `grpc-server`, tokio runtime)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Deserializes `SumRequest {a, b}`, returns `SumReply { result: a.wrapping_add(b) }` |

## Notes

- Proto message types are generated at build time from `proto/benchmark.proto` via `pb-rs` (`build.rs` → `src/grpc_bindings.rs`), not `prost`/`tonic`
- Routing uses `wtx::paths!` mapping `/benchmark.BenchmarkService/GetSum` to a `post` handler wrapped in `GrpcMiddleware`
- `HttpRecvParams::with_permissive_params()` relaxes HTTP/2 receive limits for the benchmark workload
- `run_in_threads` spreads connection handling across OS threads (one Tokio runtime per thread, no shared executor)
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
