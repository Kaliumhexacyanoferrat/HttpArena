# fastpysgi-asgi

[fastpysgi](https://github.com/remittor/fastpysgi), a libuv-based Python HTTP server, run in ASGI mode against a hand-rolled `scope`/`receive`/`send` application (no framework layer). fastpysgi manages its own worker processes and binds both listen ports itself.

## Stack

- **Language:** Python 3.13
- **Framework:** None — raw ASGI app with manual routing
- **Server:** fastpysgi (libuv), uvloop event loop
- **Interface:** ASGI
- **Build:** `python:3.13-slim`, built from git with `build-essential`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/json/<count>` | GET | Processes dataset, serializes JSON via orjson |
| `/json-comp/<count>` | GET | Same as `/json`, gzip-compressed when accepted |
| `/upload` | POST | Reads streamed ASGI body, returns byte count |
| `/static/<filename>` | GET | Serves preloaded static files with MIME types |
| `/async-db` | GET | Postgres range query with JSON response |

## Notes

- fastpysgi binds both ports itself (`add_bind('0.0.0.0', 8080)` plaintext, `8081` TLS) and manages its own worker pool via `fastpysgi.run(app, workers=WRK_COUNT)` — no external launcher process
- Event loop factory set to `uvloop.new_event_loop`; `loop_timeout=300`, backlog 16384, read buffer 256 KB, max content length ~31 MB
- Async Postgres via `asyncpg`, with `NoResetConnection` skipping the per-checkout reset query; pool created/closed via the ASGI `lifespan` protocol
- Static files preloaded into memory at startup with precomputed gzip/brotli variants, negotiated via `Accept-Encoding`
- orjson for JSON serialization; gzip (zlib, level 1) applied on demand for `/json-comp`
