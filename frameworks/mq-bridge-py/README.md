# mq-bridge-py

Python bindings for [mq-bridge](https://github.com/marcomq/mq-bridge): HTTP framing, routing configuration, and connection handling all live in Rust (`hyper-util`), while a single Python `handle(message)` function dispatches on request metadata and returns a response. Python is only on the per-request critical path for building the response body.

## Stack

- **Language:** Python 3.12
- **Framework:** mq-bridge-py (`mq_bridge_py`, Rust core via prebuilt wheel)
- **Interface:** Message-based dispatch (`mq_bridge.Message` / `mq_bridge.Route`), not WSGI/ASGI
- **Build:** `python:3.12-slim`, installs the prebuilt `mq-bridge-py` wheel from PyPI (pinned via `MQB_VERSION`)

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11`, `/baseline2` | GET | Sums two query parameters |
| `/baseline11` | POST | Sums two query parameters + request body |
| `/upload` | POST | Returns byte count of the request payload |
| `/async-db` | GET | Postgres range query with JSON response |
| `/json/{count}` | GET | Processes dataset, returns JSON (gzip'd natively above threshold) |
| `/static/{file}` | GET | Serves pre-gzipped, cached static assets |

## Notes

- Same routes are exposed on multiple listeners: plaintext HTTP/1.1 + h2 auto-negotiated on 8080, HTTP/2 cleartext (prior-knowledge only) on 8082, TLS HTTP/2 on 8443, and TLS HTTP/1.1-only on 8081 — TLS listeners only start when certs are mounted
- `json`/`json-comp` share one handler: mq-bridge's built-in `compression_enabled` (threshold 256 bytes) gzip-encodes responses when the client advertises `Accept-Encoding: gzip`
- Static assets are read and gzip-compressed once at import time into a `CachedBody` (plain + optional gzip variant), served as a dict lookup with no per-request I/O or compression
- Worker model: `os.fork()`s one process per core (override via `MQB_WORKERS`), each co-binding the same ports via `SO_REUSEPORT`; the parent is a pure supervisor that forwards `SIGTERM` and never serves requests itself
- Cyclic GC disabled (`MQ_BRIDGE_PY_GC_MODE=off`) to remove periodic collector scans from the hot path, since handlers don't create reference cycles
- Postgres via `psycopg_pool` sync connection pool; a missing `DATABASE_URL`/driver degrades `/async-db` to an empty result rather than failing
- Runs as a non-root user in the container
