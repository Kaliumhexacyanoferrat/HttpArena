# rails

Ruby on Rails (API mode) on Puma, multi-worker with one worker per CPU core.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled via Rails)
- **Framework:** Rails ~> 8.0 (`ActionController::API`, `config.api_only = true`)
- **Server:** Puma ~> 8.0, `WEB_CONCURRENCY=auto` workers, `RAILS_MAX_THREADS=3` threads/worker
- **Build:** `bundle install --deployment`, `bootsnap`, jemalloc preloaded

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text, inline lambda route) |
| `/baseline11` | GET | Sums query parameters `a` + `b` |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameters `a` + `b` |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/async-db` | GET | Postgres range query via a `ConnectionPool` of `pg` connections |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/crud/items` | GET | Paginated Postgres list by category |
| `/crud/items` | POST | Upsert item, invalidates Redis cache entry |
| `/crud/items/:id` | GET | Read item, Redis-cached with `X-Cache` header |
| `/crud/items/:id` | PUT | Update item, invalidates Redis cache entry |
| `*path` | any | Catch-all → 404 |

## Notes

- Puma bound on both plain (`8080`) and TLS (`8081`) sockets; `supported_http_methods :any` so unsupported verbs get a proper 405 instead of Puma's default 501
- Several default middleware removed (`HostAuthorization`, `Callbacks`, `RemoteIp`, `RequestId`, `Rails::Rack::Logger`, `ShowExceptions`) to cut per-request overhead
- Custom middleware marks `/upload` as `application/octet-stream` (skips form parsing) and marks `/baseline11`, `/baseline2`, `/async-db` as IO-bound so Puma can exceed its thread limit for them
- Postgres access uses prepared statements over a `ConnectionPool`; Redis-backed response cache for `/crud/items/:id`
