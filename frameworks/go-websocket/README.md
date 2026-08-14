# go-websocket

A WebSocket echo server in Go using `gobwas/ws`, a low-level, allocation-conscious WebSocket library, on top of `net/http`.

## Stack

- **Language:** Go 1.24
- **Framework:** `gobwas/ws` (+ `wsutil`)
- **Build:** Multi-stage, `golang:1.24-bookworm` → `debian:bookworm-slim` runtime, `CGO_ENABLED=0`

## What's implemented

`ws.UpgradeHTTP` performs the RFC 6455 handshake directly against the `http.ResponseWriter`/`*http.Request` pair, handing back a raw `net.Conn`. The handler then loops on `wsutil.ReadClientData`/`wsutil.WriteServerMessage`, echoing each received frame back with the same opcode until the client disconnects or an error occurs.

## Endpoint

| Method | Path | Behavior |
|--------|------|----------|
| GET | `/ws` | WebSocket upgrade, then echo every frame |

## Notes

- `GOMAXPROCS` explicitly set to `runtime.NumCPU()`
- Single process, single listener — concurrency comes from a goroutine per connection (Go's standard `net/http` connection-per-goroutine model), not from process forking or `SO_REUSEPORT`
