# dart-io

Baseline HTTP server built directly on Dart's stock `dart:io` `HttpServer`, with no framework layer. AOT-compiled and run as multiple OS processes for multi-core scaling.

## Stack

- **Language:** Dart (SDK `dart:stable`)
- **Engine:** `dart:io` `HttpServer`
- **Build:** `dart build cli` (AOT), `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums query parameter values (+ request body on POST) |
| `/json/{count}` | GET | Returns first `count` dataset items, `total = price * quantity * m` |

## Notes

- No isolates: one worker per OS process, one `HttpServer.bind(shared: true)` listener each
- Cross-process port sharing (`SO_REUSEPORT`) is enabled via an `LD_PRELOAD` shim (`reuseport_shim.c`), since Dart's `shared: true` only shares a port within a single process
- `entrypoint.sh` spawns `WORKERS` (default `nproc`) processes, each with its own event-handler thread — the same scaling model as Node.js cluster
- Server binary is AOT-compiled ahead of time via `dart build cli`
