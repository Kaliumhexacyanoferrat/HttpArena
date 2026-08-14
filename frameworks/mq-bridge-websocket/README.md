# mq-bridge-websocket

A WebSocket echo server built on **mq-bridge**, the same async message-bridging library used by the `mq-bridge` HTTP entry. A `websocket -> response` route echoes each inbound frame back on the same connection via mq-bridge's direct WebSocket execution path, without going through its general message queue.

## Stack

- **Language:** Rust 1.94
- **Framework:** mq-bridge (git dependency, pinned to tag `v0.2.21`, `websocket` feature)
- **Build:** Multi-stage, `-C target-cpu=native`, `debian:bookworm-slim` runtime

## Endpoint

| Method | Path | Description |
|--------|------|--------------|
| GET (upgrade) | `/ws` | WebSocket upgrade; echoes every inbound text/binary frame back on the same connection |

## Notes

- `WebSocketExecutionMode::DirectOnly` — frames are echoed directly on the originating connection instead of being routed through mq-bridge's general queue, cutting per-frame overhead
- Frame type (text vs. binary) is preserved via the `ws_message_type` metadata mq-bridge's WebSocket consumer attaches to each inbound frame
- The `mq-bridge` dependency is pinned to git tag `v0.2.21`

