# wtx-http2-tls

TLS variant of `wtx-http2`: the same `Http2ServerFramework` engine, terminating TLS itself and exposing only the baseline endpoint.

## Stack

- **Language:** Rust nightly-2026-06-27 (via `rustup default`, base image `rust:1.95`)
- **Framework:** wtx 0.48 (`http2-server-framework` + `crypto-ring`, tokio runtime)
- **TLS:** wtx's built-in `TlsConfig` (ring), certificate verified (`TlsModeVerified`)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums query parameter values, returned as plain text |

## Notes

- Certificate/key are read from `TLS_CERT`/`TLS_KEY` env vars (default `/certs/server.crt` / `/certs/server.key`), loaded as PEM into `TlsConfig::from_keys_pem`
- Listens on `:8443`; `HttpRecvParams::with_permissive_params()` relaxes HTTP/2 receive limits
- `run_in_threads` spreads connection handling across OS threads (one Tokio runtime per thread, no shared executor)
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
