# bun-websocket

A WebSocket echo server on Bun's native `Bun.serve` WebSocket API — no external WebSocket library.

## Stack

- **Language:** TypeScript
- **Runtime:** Bun (`oven/bun:latest`)
- **Build:** Single-stage, runs directly from source (no compile step)

## What's implemented

`Bun.serve` handles the HTTP-to-WebSocket upgrade for `GET /ws` via `server.upgrade(req)`; any other path returns `404`, and a failed upgrade returns `400`. The `websocket.message` handler echoes each incoming message back verbatim (`ws.send(message)`) — framing, ping/pong, and close are all handled by Bun's runtime.

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every message |

## Notes

- `entrypoint.sh` forks one `bun run server.ts` process per CPU, each binding port 8080 with `reusePort: true` (`SO_REUSEPORT`) so the kernel load-balances connections across processes
- No clustering/IPC between processes — each is an independent event loop
