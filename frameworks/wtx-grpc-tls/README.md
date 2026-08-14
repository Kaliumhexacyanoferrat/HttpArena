# wtx-grpc-tls

TLS variant of `wtx-grpc`: the same `wtx` `Http2ServerFramework` gRPC server, but terminating rustls/ring TLS itself instead of serving cleartext.

## Stack

- **Language:** Rust nightly-2026-06-27 (via `rustup default`, base image `rust:1.95`)
- **Framework:** wtx 0.48 (`http2-server-framework` + `grpc-server` + `crypto-ring`, tokio runtime)
- **TLS:** wtx's built-in `TlsConfig` (ring), certificate verified (`TlsModeVerified`)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Deserializes `SumRequest {a, b}`, returns `SumReply { result: a.wrapping_add(b) }` |

## Notes

- Certificate/key are read from `TLS_CERT`/`TLS_KEY` env vars (default `/certs/server.crt` / `/certs/server.key`), loaded as PEM into `TlsConfig::from_keys_pem`
- Proto message types are generated at build time from `proto/benchmark.proto` via `pb-rs` (`build.rs` → `src/grpc_bindings.rs`)
- `set_error_cb` prints connection/protocol errors to stderr, useful since TLS failures would otherwise be silent
- Listens on `:8443`; `HttpRecvParams::with_permissive_params()` relaxes HTTP/2 receive limits
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
