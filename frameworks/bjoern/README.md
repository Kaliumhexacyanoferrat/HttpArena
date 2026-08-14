# bjoern

Bjoern is a minimal, libev-based WSGI server written in C. This entry pairs it with a hand-rolled WSGI application (no framework layer) that implements routing directly against `environ`/`start_response`.

## Stack

- **Language:** Python 3.9
- **Framework:** None — raw WSGI callable with manual routing
- **Server:** bjoern (libev)
- **Build:** `python:3.9-bullseye`, `libev-dev` for the native extension

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/<count>` | GET | Processes dataset, serializes JSON via orjson |
| `/json-comp/<count>` | GET | Same as `/json`, gzip-compressed when accepted |
| `/upload` | POST | Streams request body, returns byte count |
| `/static/<filename>` | GET | Serves preloaded static files with MIME types |
| `/async-db` | GET | Postgres range query with JSON response |

## Notes

- Process model: `os.fork()` spins up `WRK_COUNT - 1` extra workers (each running `bjoern.run(..., reuse_port=True)`), plus the main process also serves requests
- Listen backlog set to 16384
- Static files preloaded into memory at startup, with precomputed gzip/brotli variants selected via `Accept-Encoding`
- Postgres access via `psycopg_pool` (sync connection pool), pool size derived from `DATABASE_MAX_CONN` divided across workers
- orjson used for JSON serialization; gzip compression (level 1) applied on demand for `/json-comp`
