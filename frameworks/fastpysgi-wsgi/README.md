# fastpysgi-wsgi

[fastpysgi](https://github.com/remittor/fastpysgi), a libuv-based Python HTTP server, run in WSGI mode against a hand-rolled `environ`/`start_response` application (no framework layer). Same underlying engine as `fastpysgi-asgi`, in its synchronous interface.

## Stack

- **Language:** Python 3.13
- **Framework:** None — raw WSGI callable with manual routing
- **Server:** fastpysgi (libuv)
- **Interface:** WSGI
- **Build:** `python:3.13-slim-bookworm`, built from git with `build-essential`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + first 100 bytes of body |
| `/json/<count>` | GET | Processes dataset, serializes JSON via orjson |
| `/json-comp/<count>` | GET | Same as `/json`, gzip-compressed when accepted |
| `/upload` | POST | Returns byte count from the buffered `wsgi.input` |
| `/static/<filename>` | GET | Serves preloaded static files with MIME types |
| `/async-db` | GET | Postgres range query with JSON response |

## Notes

- fastpysgi binds both ports itself (`add_bind('0.0.0.0', 8080)` plaintext, `8081` TLS) and manages its own worker pool via `fastpysgi.run(app, workers=WRK_COUNT)` — no external launcher process, no uvloop (sync WSGI mode)
- `/upload` reads the size directly off `wsgi.input.getbuffer().nbytes` rather than streaming reads, since fastpysgi buffers the WSGI input itself
- Postgres access via `psycopg_pool` (sync connection pool), pool size derived from `DATABASE_MAX_CONN`
- Static files preloaded into memory at startup with precomputed gzip/brotli variants, negotiated via `Accept-Encoding`
- orjson for JSON serialization; gzip (zlib, level 1) applied on demand for `/json-comp`
- Backlog 16384, read buffer 256 KB, max content length ~31 MB
