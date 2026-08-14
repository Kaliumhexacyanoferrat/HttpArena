# node-websocket

A WebSocket echo server on Node.js using the `ws` package, clustered across CPU cores.

## Stack

- **Language:** JavaScript
- **Runtime:** Node.js 22 (`node:22-slim`)
- **Framework:** `ws` 8.18
- **Build:** Single-stage, `npm install --omit=dev`

## What's implemented

The primary process forks one worker per CPU core via Node's `cluster` module; each worker runs its own `http.Server` and a `WebSocketServer` created with `noServer: true`. The server's `upgrade` event checks the path is `/ws`, then calls `wss.handleUpgrade` to complete the WebSocket handshake; any other path destroys the socket. Each connection's `message` handler echoes the payload back verbatim (`ws.on('message', msg => ws.send(msg))`).

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every message |

## Notes

- Uses Node's built-in `cluster` module (not `SO_REUSEPORT` directly) — the primary process distributes incoming connections to workers via the cluster's internal round-robin/shared-handle balancing
- Worker count is `os.availableParallelism()` (falls back to `os.cpus().length`)
