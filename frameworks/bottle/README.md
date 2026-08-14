# bottle

Bottle, a single-file WSGI micro-framework, served by Gunicorn with synchronous workers. `launcher.py` runs two Gunicorn instances side by side, one for cleartext HTTP and one for TLS.

## Stack

- **Language:** Python 3.13
- **Framework:** Bottle
- **Server:** Gunicorn (`worker_class = sync`)
- **Interface:** WSGI
- **Build:** `python:3.13-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums two query parameters |
| `/baseline11` | POST | Sums two query parameters + first 100 bytes of body |
| `/json/<count:int>` | GET | Processes dataset, returns JSON |
| `/async-db` | GET | Postgres range query with JSON response |
| `/upload` | POST | Streams request body, returns byte count |
| `/static/<filepath:path>` | GET | Serves files from `/data/static` via `static_file()` |

## Notes

- `launcher.py` spawns two Gunicorn processes from `gunicorn_conf.py` / `gunicorn_conf_ssl.py` (HTTP on 8080, TLS on 8081), forwarding `SIGTERM`/`SIGINT` to both
- Gunicorn workers = detected CPU count (floor 4, cap 128), sync worker class, `keepalive = 120`
- `before_request` hook manually buffers chunked request bodies into memory, since Gunicorn's sync workers don't decode `Transfer-Encoding: chunked`
- `MEMFILE_MAX` raised to 31 MB so uploads stay in memory instead of spooling to disk
- Postgres access via `psycopg_pool` sync connection pool, sized from `DATABASE_MAX_CONN`
- `Server` header overridden to `Bottle` at the Gunicorn level
