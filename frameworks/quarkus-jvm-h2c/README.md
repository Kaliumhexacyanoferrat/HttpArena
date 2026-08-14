# quarkus-jvm-h2c

Standalone Vert.x 4.5 server on JDK 25, packaged as a shaded fat jar (Quarkus proper is not used). One Vert.x instance is created per CPU so each `HttpServer` binds its own listening socket with `SO_REUSEPORT` via the Netty epoll native transport, letting the kernel distribute accepts across cores. The listener is h2c-only: any request that doesn't negotiate as HTTP/2 is rejected with `400`.

## Stack

- **Language:** Java 21 (compiled), running on JDK 25 at runtime
- **Framework:** none — raw Vert.x Core (`io.vertx.core.http`), no Quarkus runtime
- **Engine:** Vert.x / Netty epoll native transport (same HTTP stack Quarkus uses underneath, run standalone)
- **Build:** `maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums `a`+`b` query params (defaults to 0 if missing/unparsable) |
| `/json/{count}` | GET | Renders up to `count` items (clamped to dataset size) from `DATASET_PATH` (default `/data/dataset.json`) as JSON, with each item's `total = price * quantity * m` (`m` query param, default 1) |

## Notes

- Quarkus's package step strips classifier-carrying jars like `netty-transport-native-epoll` from the runtime image even when declared as a dependency, which makes `SO_REUSEPORT` a no-op — this framework drops Quarkus packaging and ships a standalone shaded jar so the native transport actually loads.
- A single shared `Vertx` instance would fold multiple `HttpServer`s on the same port into one listening socket even with `setReusePort(true)`; instead, one `Vertx` instance per CPU (each with `setEventLoopPoolSize(1)`) is created so each binds independently and the kernel-level `SO_REUSEPORT` fan-out actually kicks in.
- Any request that isn't HTTP/2 (i.e. `req.version() != HttpVersion.HTTP_2`) is rejected with `400 HTTP/2 cleartext prior-knowledge required` — an anti-cheat check ensuring the h2c-only port isn't served over HTTP/1.1.
- Listens in plaintext on port 8082 (`setUseAlpn(false)`, `setSsl(false)`, `setHttp2ClearTextEnabled(true)`).
