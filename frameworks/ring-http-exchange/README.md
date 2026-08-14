# ring-http-exchange

Async Ring handler served through the `ring-http-exchange` library, using the [robaho/httpserver](https://github.com/robaho/httpserver) implementation (a faster drop-in replacement for the JDK's built-in `com.sun.net.httpserver`) instead of the default JDK HTTP server. Requests are dispatched through the same lightweight custom `tassu` router used by the `aleph` entry, executed on virtual threads.

## Stack

- **Language:** Clojure 1.12 (Leiningen)
- **Framework:** `ring-http-exchange` 1.4.5 (`jj.tassu` async router)
- **Server:** robaho/httpserver 1.0.29 (alternative `HttpServer` implementation, via `ring-http-exchange.core`)
- **Build:** `clojure:temurin-26-lein-trixie` (uberjar) → `eclipse-temurin:26-jre` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/` | GET | Returns server name (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON (manual gzip if `Accept-Encoding` allows) |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/async-db` | GET | Async Postgres range query via Vert.x reactive client |
| `/fortunes` | GET | Renders fortunes table (majavat template) with an extra runtime-added row |
| `/crud/items` | GET | Paginated Postgres list by category |
| `/crud/items` | POST | Insert/upsert item |
| `/crud/items/:id` | GET | Read item by id (200ms TTL cache, `X-Cache` header) |
| `/crud/items/:id` | PUT | Update item, evicts cache entry |
| `/static/:filename` | GET | Serves files from `/data/static` |

## Notes

- Runs on a `Executors/newVirtualThreadPerTaskExecutor` passed to the server as its request executor
- Async Postgres access via `jj.sql.async-boa` / Vert.x reactive Postgres client, not JDBC
- `/json/:count` gzips its own response body based on `Accept-Encoding`, rather than relying on server-level compression
- CRUD reads cached in a 200ms TTL cache with `X-Cache: HIT`/`MISS` headers
- TLS (port 8081) built from PEM cert/key into a PKCS12 keystore via `java.security.PEMDecoder`
