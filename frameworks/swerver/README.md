# swerver

The `swerver` Zig framework run as an engine-tier entry: pre-computed response bodies disabled, JSON serialized per request through `std.json`, static files served from an in-memory cache with precompressed `.br`/`.gz` negotiation, and a native async PostgreSQL client (pipelined, prepared-statement cache, park/resume) for the database endpoints.

## Stack

- **Language:** Zig (vendored `swerver` v0.1.0-alpha.24)
- **Framework:** swerver
- **Build:** Multi-stage, `debian:trixie` build, `debian:trixie-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Empty 200 response |
| `/echo` | GET | Returns `{"status":"ok"}` |
| `/echo` | POST | Echoes the request body back |
| `/plaintext` | GET | Returns `Hello, World!` |
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET, POST | Sums query parameter values (plus POST body as an integer) |
| `/baseline2` | GET, POST | Same as `/baseline11`, HTTP/2 variant |
| `/json/:count` | GET | Renders `count` dataset items with a per-request `total`; gzips when the client accepts it (json-comp) |
| `/upload` | POST | Returns the received byte count (body discarded, not buffered) |
| `/async-db` | GET | Range query over `items` (`price BETWEEN min AND max LIMIT n`) via async, park-and-resume PostgreSQL |
| `/fortunes` | GET | All `fortune` rows plus one injected at request time, sorted and HTML-escaped into a table |

## Notes

- One multi-listener process serves all four protocol ports at once: HTTP/1.1 (8080), h2c (8082), TLS/HTTP2 (8081), and HTTP/3 over QUIC (8443) — config in `config-multi.json`
- Single-threaded event loop per worker with `SO_REUSEPORT` fork fan-out; kqueue/epoll/io_uring backends depending on platform
- Async PostgreSQL access uses swerver's handler_api "park sentinel" pattern: a handler issues a query and returns a park marker, and a continuation renders the response once rows arrive
- JSONB columns come back with a 1-byte version prefix (`0x01`) that is stripped before re-parsing as JSON
- The `/fortunes` HTML table is rendered into the resume arena (not the fixed `response_buf`) since the ~200-row table can exceed 24 KB
