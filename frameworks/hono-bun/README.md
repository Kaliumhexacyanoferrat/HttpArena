# hono-bun

Hono, a Web Standards-based framework, running natively on Bun with no Node adapter layer — the fetch handler is passed straight to `Bun.serve`.

## Stack

- **Language:** TypeScript
- **Framework:** Hono 4.7
- **Runtime:** Bun (`oven/bun:latest`, JavaScriptCore)
- **Build:** Single-stage, `bun install --production`, runs from source

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/:count` | GET | Renders `count` dataset items with a per-request `total`; compressed via Hono's `compress()` middleware when accepted |
| `/db` | GET | Read-only SQLite (`bun:sqlite`) range query over `items` |
| `/async-db` | GET | PostgreSQL range query over `items` via a `pg` connection pool |
| `/upload` | POST | Streams the request body, returns byte count |
| `/static/:filename` | GET | Serves `/data/static` via `Bun.file`, content type by extension |
| `/crud/items` | GET | Paginated list by category, always hits PostgreSQL |
| `/crud/items/:id` | GET | Single-item read, cached (200ms TTL, Redis if `REDIS_URL` set, else in-process `Map`) |
| `/crud/items` | POST | Upsert (`INSERT ... ON CONFLICT DO UPDATE`), 201 |
| `/crud/items/:id` | PUT | Update, invalidates the item's cache entry |

## Notes

- One Bun process per CPU (`entrypoint.sh`, cgroup-aware CPU count), all binding `reusePort: true` on 8080 for kernel-level load balancing
- PostgreSQL pool is per-process (`max: 8`), so total connections scale with core count — sized to roughly match `aspnet-minimal`'s Npgsql pool for cross-framework comparison
- CRUD single-item cache uses Redis when `REDIS_URL` is set (shared across processes); otherwise falls back to a process-local `Map`, which loses hit-rate consistency under `SO_REUSEPORT` fan-out
- Cached CRUD values are stored pre-serialized as JSON strings to skip a parse+stringify round trip on hits
