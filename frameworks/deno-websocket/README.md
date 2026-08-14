# deno-websocket

A WebSocket echo server on Deno's native `Deno.upgradeWebSocket` API — no external WebSocket library.

## Stack

- **Language:** TypeScript
- **Runtime:** Deno 2.2.2 (`denoland/deno:2.2.2`)
- **Build:** Single-stage, `deno cache` at build time, served directly from source

## What's implemented

The default `fetch` handler checks for `/ws` and calls `Deno.upgradeWebSocket(req)`, which returns a `{ socket, response }` pair — the response completes the HTTP upgrade and `socket` is the resulting WebSocket. `socket.onmessage` echoes each event's data back verbatim. Any other path returns `404`.

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every message |

## Notes

- Runs via `deno serve --parallel --port 8080 --host 0.0.0.0 -A server.ts`, which spawns one worker per CPU core internally rather than relying on external process forking
- Framing, ping/pong, and close are handled entirely by the Deno runtime
