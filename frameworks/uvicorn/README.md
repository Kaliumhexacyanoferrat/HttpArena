# uvicorn

Uvicorn, an ASGI server built on uvloop, run directly against a hand-rolled `scope`/`receive`/`send` application (no framework layer). `launcher.py` runs separate Uvicorn processes for cleartext and TLS.

## Stack

- **Language:** Python 3.13
- **Framework:** None — raw ASGI app with manual routing
- **Server:** Uvicorn (uvloop)
- **Interface:** ASGI
- **Build:** `python:3.13-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/<count>` | GET | Processes dataset, serializes JSON via orjson |
| `/json-comp/<count>` | GET | Same as `/json`, gzip-compressed when accepted |
| `/upload` | POST | Reads streamed ASGI body, returns byte count |
| `/static/<filename>` | GET | Serves preloaded static files with MIME types |
| `/async-db` | GET | Postgres range query with JSON response |

## Notes

- `launcher.py` spawns two Uvicorn subprocesses (HTTP on 8080, HTTPS on 8081 with `--ssl-certfile`/`--ssl-keyfile`), adding `--workers <core count>` if not already given, and forwards `SIGTERM`/`SIGINT` to both
- Same hand-rolled ASGI routing pattern as `fastpysgi-asgi`, but served by stock Uvicorn instead of the fastpysgi engine
- Async Postgres via `asyncpg`, with `NoResetConnection` skipping the per-checkout reset query; pool created/closed via the ASGI `lifespan` protocol
- Static files preloaded into memory at startup with precomputed gzip/brotli variants, negotiated via `Accept-Encoding`
- orjson for JSON serialization; gzip (zlib, level 1) applied on demand for `/json-comp`
