# sinatra

Sinatra DSL web framework on Puma, multi-threaded with one worker per CPU core.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled)
- **Framework:** Sinatra ~> 4.1 (`Sinatra::Base`, classless style)
- **Server:** Puma ~> 8.0, `MAX_THREADS=3` / `MAX_IO_THREADS=10`
- **Build:** `bundle install --deployment`, jemalloc preloaded

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameters `a` + `b` |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameters `a` + `b` |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/async-db` | GET | Postgres range query via a `ConnectionPool` of `pg` connections |
| `/crud/items` | GET | Paginated Postgres list by category |
| `/crud/items` | POST | Upsert item, invalidates Redis cache entry |
| `/crud/items/:id` | GET | Read item, Redis-cached with `X-Cache` header |
| `/crud/items/:id` | PUT | Update item, invalidates Redis cache entry |

## Notes

- Static files served via Sinatra's built-in `static` setting, `public_folder` pointed at `DATA_DIR`
- `Sinatra::Request#form_data?` overridden so an untyped POST body on `/upload` isn't misparsed as form data
- Protections (CSRF, etc.) and host authorization disabled for benchmark purposes
- Postgres access uses prepared statements over a `ConnectionPool`; Redis-backed response cache for `/crud/items/:id`
