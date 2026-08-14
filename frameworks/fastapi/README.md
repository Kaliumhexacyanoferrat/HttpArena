# fastapi

FastAPI, an async ASGI web framework built on Starlette/Pydantic, served by Uvicorn with the uvloop event loop. `launcher.py` runs separate Uvicorn processes for cleartext and TLS.

## Stack

- **Language:** Python 3.13
- **Framework:** FastAPI (Starlette)
- **Server:** Uvicorn (uvloop)
- **Interface:** ASGI
- **Build:** `python:3.13-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET, POST | Sums query parameters (+ body on POST) |
| `/json/{count}` | GET | Processes dataset, returns JSON |
| `/json-comp/{count}` | GET | Same handler, response gzip'd by middleware |
| `/async-db` | GET | Postgres range query with JSON response |
| `/upload` | POST | Streams request body, returns byte count |
| `/static/*` | GET | Served via `StaticFiles` mount on `/data/static` |

## Notes

- `launcher.py` spawns two Uvicorn subprocesses (HTTP on 8080, HTTPS on 8081 with `--ssl-certfile`/`--ssl-keyfile`), adding `--workers <core count>` if not already given, and forwards `SIGTERM`/`SIGINT` to both
- `GZipMiddleware` applied globally (`minimum_size=1000`, `compresslevel=5`), so `/json` and `/json-comp` share one handler
- Postgres pool created via `asyncpg` in the app's `lifespan` context manager; `NoResetConnection` skips the per-checkout reset query, pool sized from `DATABASE_MAX_CONN`
- Static files served per-request from disk via `StaticFiles`, unlike the memory-preloading approach used by the hand-rolled ASGI entries
- `Server` response header pinned to `FastAPI` via a custom Uvicorn header flag
