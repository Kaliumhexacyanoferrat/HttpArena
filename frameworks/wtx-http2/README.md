# wtx-http2

Plain HTTP/2 cleartext server on `wtx`'s `Http2ServerFramework`, implementing the same baseline/JSON handlers as the other h2c framework entries.

## Stack

- **Language:** Rust nightly-2026-06-27 (via `rustup default`, base image `rust:1.95`)
- **Framework:** wtx 0.48 (`http2-server-framework`, tokio runtime)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums query parameter values, returned as plain text |
| `/json/{count}` | GET | Processes up to `count` dataset items (scaled by `m`), serializes JSON |

## Notes

- Dataset is loaded once from `DATASET_PATH` (default `/data/dataset.json`) into an `Arc<Vector<DatasetItem>>` shared across connections via a `wtx::Lease`-derived `ConnAux`
- JSON serialization streams directly into the response body buffer (`serde_json::to_writer`) rather than building an intermediate `Vec`
- `HttpRecvParams::with_permissive_params()` relaxes HTTP/2 receive limits for the benchmark workload
- `run_in_threads` spreads connection handling across OS threads (one Tokio runtime per thread, no shared executor)
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
