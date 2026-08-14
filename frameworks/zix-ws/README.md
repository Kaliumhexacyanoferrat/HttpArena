# zix-ws

A WebSocket echo server on the `zix.Http1` raw engine (no `std.http`), where the upgrade handshake and echo loop are engine-owned rather than hand-rolled in the application. Shared-nothing by design: each worker owns its own `SO_REUSEPORT` multishot accept, io_uring completion ring, and connections.

## Stack

- **Language:** Zig 0.16.0
- **Engine:** zix (`zix.Http1.WebSocket`, `.URING` dispatch model)
- **Build:** Multi-stage, `alpine:3.20` build and runtime, musl target with `x86_64_v3`/`baseline` CPU tuning

## What's implemented

The application registers a single upgrade handler that validates the `Upgrade: websocket` header and `Sec-WebSocket-Key`, then hands the connection to `zix.Http1.WebSocket.serve`, which owns the RFC 6455 handshake and frame loop inside the engine. The application-level callback (`wsOnFrame`) only echoes each text/binary frame's payload back verbatim — ping/pong and close are handled entirely by the engine.

Frames are read from a shared per-worker provided-buffer ring (an idle connection holds no buffer), and a pipelined burst of frames is coalesced into a single write.

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every frame |

A non-upgrade request or any other path returns `400`/`404`.

## Notes

- No response cache: echo is per-connection, not a broadcast fanout, so there is nothing to precompute or share across connections
- Two tuning profiles (`lean`/`throughput`) size the HTTP handshake recv buffer for dev vs. competition hardware; the 32 KiB WebSocket frame buffer is fixed
- Process priority elevated via `setpriority(-19)`, best-effort
