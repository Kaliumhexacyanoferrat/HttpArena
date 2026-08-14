# flask

Flask, a WSGI micro-framework, served by Gunicorn with synchronous workers. `launcher.py` runs two Gunicorn instances side by side, one for cleartext HTTP and one for TLS.

## Stack

- **Language:** Python 3.13
- **Framework:** Flask
- **Server:** Gunicorn (`worker_class = sync`)
- **Interface:** WSGI
- **Build:** `python:3.13-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET, POST | Sums query parameters (+ body on POST) |
| `/json/<int:count>` | GET | Processes dataset, returns JSON |
| `/json-comp/<int:count>` | GET | Same handler, response gzip'd by `after_request` |
| `/async-db` | GET | Postgres range query with JSON response |
| `/upload` | POST | Streams request body, returns byte count |
| `/static/<path:filepath>` | GET | Served via `send_from_directory('/data/static', ...)` |

## Notes

- `launcher.py` spawns two Gunicorn processes from `gunicorn_conf.py` / `gunicorn_conf_ssl.py` (HTTP on 8080, TLS on 8081), forwarding `SIGTERM`/`SIGINT` to both
- Gunicorn workers = detected CPU count (floor 4, cap 128), sync worker class, `keepalive = 120`
- `after_request` hook gzip-compresses eligible responses (level 5) only when the client sends `Accept-Encoding: gzip` and no encoding is already set
- Postgres access via `psycopg_pool` sync connection pool, sized from `DATABASE_MAX_CONN`
- Static files served per-request from disk (not preloaded), with `.woff2`/`.webp` MIME types registered
- `Server` header overridden to `Flask` at the Gunicorn level
