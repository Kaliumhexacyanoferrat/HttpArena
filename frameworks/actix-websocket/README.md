# actix-websocket

Actix-web 4 WebSocket echo server built on `actix-ws`, used as the framework-tier counterpart to the raw-tokio `tokio-ws` engine entry.

## Stack

- **Language:** Rust (edition 2021)
- **Framework:** Actix-web 4, `actix-ws` 0.3
- **Build:** Single-stage `cargo build --release`, `debian:bookworm-slim` runtime

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every frame |

## Notes

- Each connection's message loop runs in its own `actix_web::rt::spawn`ed task, reading via `actix_ws::MessageStream`
- `Text` and `Binary` frames are echoed back as-is; `Ping` is answered with `Pong`; `Close` ends the loop
- No custom `[profile.release]` (no LTO / `target-cpu` tuning) and no worker count override — uses Actix's default worker pool
