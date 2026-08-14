# wtx-ws

WebSocket echo server on `wtx`'s `WebSocketServerFramework`, the framework-tier counterpart to the hand-rolled `tokio-ws` engine entry.

## Stack

- **Language:** Rust nightly-2026-06-27 (via `rustup default`, base image `rust:1.95`)
- **Framework:** wtx 0.48 (`web-socket-server-framework` + `crypto-ring`, tokio executor)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every frame |

## Notes

- `Text`/`Binary` frames are echoed back unchanged via `read_frame`/`write_frame`; `Close` ends the connection; other opcodes (including `Ping`) are silently ignored, so no automatic `Pong` is sent
- `TcpParams::default().tcp_nodelay(false)` explicitly leaves Nagle's algorithm enabled, unlike most other engine/framework entries which disable it
- `run_in_threads` spreads connection handling across OS threads on a `TokioExecutor`
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
