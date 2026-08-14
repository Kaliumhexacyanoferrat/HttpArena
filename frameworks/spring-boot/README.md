# spring-boot

Spring Boot with embedded Tomcat on JDK 25, virtual threads enabled for MVC request handling. Serves multiple listener/protocol combinations (plaintext HTTP/1.1, HTTP/1.1+TLS, and HTTP/2 over TLS) plus SQLite/PostgreSQL-backed JSON endpoints and a WebSocket echo handler.

## Stack

- **Language:** Java 25
- **Framework:** Spring Boot (Spring MVC, servlet stack)
- **Engine:** Tomcat (embedded), virtual threads (`spring.threads.virtual.enabled=true`)
- **Build:** multi-stage, `eclipse-temurin:25` (Maven wrapper build) → `eclipse-temurin:25-jre` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`+`b` query params |
| `/baseline11` | POST | Sums query params plus an integer parsed from the request body |
| `/baseline2` | GET | Sums `a`+`b` query params |
| `/json/{count}` | GET | Renders up to `count` items from a preloaded dataset as JSON; optional `m` query multiplier (default 1) |
| `/db` | GET | SQLite range query (`price BETWEEN min AND max`, `min`/`max` query params, default 10/50), limit 50, JSON response |
| `/async-db` | GET | Same shape against PostgreSQL via HikariCP; only registered if `DATABASE_URL` (`httparena.postgres-url`) is set; adds a `limit` query param (default/clamped to 50) |
| `/upload` | POST | Drains the request body and returns its byte count |
| `/static/**` | GET | Serves static files from `/data/static/` |
| `/ws` | WebSocket | Echoes every received message back to the client |

## Notes

- Main connector is HTTPS/HTTP2 on port 8443 (`server.http2.enabled=true`, TLS from `/certs/server.crt`+`/certs/server.key`, `TLSv1.3` only); an additional plaintext HTTP/1.1 connector is added on port 8080, and — only if the cert/key files exist on disk — a second TLS HTTP/1.1 connector on port 8081.
- Response compression is enabled server-wide (`server.compression.enabled=true`).
- SQLite datasource (`/data/benchmark.db`) is read-only and always configured; the PostgreSQL datasource/pool (HikariCP) is conditional on `httparena.postgres-url` being set, gating both the datasource beans and the `/async-db` controller via `@ConditionalOnProperty`.
- Uses `tools.jackson` (Jackson 3) `JsonMapper` rather than the classic `com.fasterxml.jackson.databind.ObjectMapper`.
