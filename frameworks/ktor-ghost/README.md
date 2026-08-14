# ktor-ghost

JetBrains Ktor 3.x on Netty with Kotlin coroutines and JDK 21, using the Ghost serializer in place of kotlinx.serialization for all JSON encode/decode. Otherwise identical routing and application logic to the `ktor` entry — this variant isolates the effect of the serialization library.

## Stack

- **Language:** Kotlin
- **Framework:** Ktor 3.x (routing DSL)
- **Engine:** Netty
- **Serialization:** Ghost (`com.ghost.serialization.Ghost`), pre-warmed at startup
- **Build:** `gradle:9.5.1-jdk21-corretto` → `eclipse-temurin:21-jre` runtime, fat jar

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/{count}` | GET | Returns `count` items from the dataset with `total = price * quantity * m`, encoded via Ghost; gzip via `Compression` plugin |
| `/async-db` | GET | Postgres range query via Exposed: `price BETWEEN min AND max LIMIT limit` |
| `/upload` | POST | Streams the request body to a no-op sink, returns byte count |
| `/static/*` | GET | Serves `/data/static`, preferring precompressed `.br`/`.gz` sidecars |
| `/ws` | GET (upgrade) | WebSocket echo — re-sends every incoming frame |
| `/crud/items` | GET | Paginated list by category |
| `/crud/items/{id}` | GET | Single item read, in-process cache, `X-Cache: HIT/MISS` |
| `/crud/items` | POST | Upsert via Exposed `upsert`, returns 201 |
| `/crud/items/{id}` | PUT | Partial update, invalidates cache entry |
| `/fortunes` | GET | Server-rendered HTML via the kotlinx.html DSL |

## Notes

- `Ghost.prewarm()` runs before the server starts, ahead-of-time initializing whatever the serializer needs (codegen/reflection caches) so the first request isn't penalized
- All JSON I/O goes through `respondGhost`/`receiveGhost` helpers (`Ghost.encodeToBytes` / `Ghost.deserialize`) instead of Ktor's `ContentNegotiation` plugin — no `install(ContentNegotiation)` anywhere in the routes
- Two embedded Netty servers run side by side: the main one on 8080 (h1) / 8081 (TLS h1) / 8443 (TLS h1+h2), and a second dedicated H2C server on 8082 that rejects any request not already negotiated as HTTP/2
- `sumQueryParams()` hand-parses the raw query string instead of going through Ktor's parameter map
- `/crud/items/{id}` caches the Ghost-encoded bytes directly, so a cache hit skips both DB and serialization
- JVM tuned for throughput: G1GC, NUMA-aware, pre-touched heap, Netty leak detection disabled, bounded-capacity object recycler, native transport enabled

