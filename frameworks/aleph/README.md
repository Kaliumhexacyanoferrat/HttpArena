# aleph

Aleph (Netty-based async Clojure HTTP server) benchmark entry. Requests are dispatched through a lightweight custom `tassu` router directly on Netty's event-loop threads (no per-request thread pool), with async Postgres access via a Vert.x reactive client.

## Stack

- **Language:** Clojure 1.12 (Leiningen)
- **Framework:** Aleph 0.8.3 (`jj.tassu` router on top of `aleph.http`)
- **Server:** Netty (embedded via Aleph), raw-stream mode
- **Build:** `clojure:temurin-26-lein-trixie` (uberjar) → `eclipse-temurin:26-jre` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/` | GET | Returns server name (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/json` | GET | Pre-rendered JSON dataset |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/compression` | GET | Pre-rendered large JSON dataset (served with gzip) |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/async-db` | GET | Async Postgres range query via Vert.x reactive client |
| `/crud/items` | GET | Paginated Postgres list by category |
| `/crud/items` | POST | Insert/upsert item |
| `/crud/items/:id` | GET | Read item by id (200ms TTL cache, `X-Cache` header) |
| `/crud/items/:id` | PUT | Update item, evicts cache entry |
| `/fortunes` | GET | Renders fortunes table (Hiccup HTML) with an extra runtime-added row |
| `/static/:filename` | GET | Serves files from `/data/static` |

## Notes

- Handler runs with `:executor :none`, `:raw-stream? true` — request handling happens directly on Netty I/O threads
- Async Postgres access via `jj.sql.async-boa` / Vert.x reactive Postgres client, not JDBC
- CRUD reads cached in a 200ms TTL cache with `X-Cache: HIT`/`MISS` headers
- Gzip added to the Netty pipeline via `HttpContentCompressor`
- TLS (port 8081) built from PEM cert/key via a manually constructed Netty `SslContext`, HTTP/1.1 only
- Netty leak detector explicitly disabled
