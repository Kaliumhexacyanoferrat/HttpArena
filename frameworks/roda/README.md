# roda

Roda (routing-tree toolkit) application on Puma, multi-worker with one worker per CPU core.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled)
- **Framework:** Roda ~> 3.102
- **Server:** Puma ~> 8.0, `MAX_THREADS=4` / `MAX_IO_THREADS=10`
- **Build:** `bundle install --deployment`, jemalloc preloaded

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/` | GET | Returns `ok` |
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameters `a` + `b` |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameters `a` + `b` |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/upload` | POST | Reads and discards request body, marks request as IO-bound |
| `/async-db` | GET | Postgres range query via a `ConnectionPool` of `pg` connections |
| `/crud/items` | GET | Paginated Postgres list by category |
| `/crud/items` | POST | Upsert item, invalidates Redis cache entry |
| `/crud/items/:id` | GET | Read item, Redis-cached with `X-Cache` header |
| `/crud/items/:id` | PUT | Update item, invalidates Redis cache entry |
| `/*` | GET | Falls through to `plugin :public`, serving static files from `DATA_DIR` (gzip/brotli) |

## Notes

- Static file serving uses Roda's `public` plugin with `gzip: true, brotli: true` pre-compression support
- `/upload` explicitly calls `puma.mark_as_io_bound` so Puma can exceed its thread limit for that request
- Postgres access uses prepared statements over a `ConnectionPool`; Redis-backed response cache for `/crud/items/:id`
- Puma bound on both plain (`8080`) and TLS (`8081`) sockets
