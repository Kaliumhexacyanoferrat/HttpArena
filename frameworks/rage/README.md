# rage

Rage, a Rails-inspired API framework built for non-blocking I/O, running on the Iodine server. Implements the same baseline/JSON/CRUD endpoint surface as the `rails` and `sinatra` entries for comparison.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled)
- **Framework:** rage-rb ~> 1.22
- **Server:** Iodine (`bundle exec rage server`)
- **Build:** `bundle install --deployment`, jemalloc preloaded

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
| `*` | any | Catch-all → 404 |

## Notes

- `Rack::Deflater` enabled for gzip; static files served via Rage's built-in `public_file_server`
- Postgres access uses prepared statements over a `ConnectionPool`, sized from `2 * log(256 / cpu_count)`
- Redis-backed response cache for `/crud/items/:id`, invalidated on create/update
- Custom monkey-patch of `Rage::ParamsParser` to correctly handle non-multipart POST bodies
