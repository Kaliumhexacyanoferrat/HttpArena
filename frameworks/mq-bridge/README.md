# mq-bridge

mq-bridge is an async message-bridging library run here as an HTTP server. A single catch-all `http -> response` route dispatches on request metadata (method, path, query) and replies through mq-bridge's inline-response fast path. hyper-util's auto connection builder serves HTTP/1.1 and HTTP/2 prior-knowledge (h2c) on the cleartext port, plus dedicated h2c-only, HTTP/1.1-over-TLS, and HTTP/2-over-TLS listeners.

## Stack

- **Language:** Rust 1.94
- **Framework:** mq-bridge (git dependency, pinned to tag `v0.2.21`)
- **TLS:** rustls (ring crypto provider)
- **DB:** sqlx (Postgres, `runtime-tokio-rustls`)
- **Build:** Multi-stage, `-C target-cpu=native`, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`/`b` query parameter values |
| `/baseline11` | POST | Sums `a`/`b` query parameters + request body |
| `/baseline2` | GET | Same as `/baseline11` GET (h2/h2c variant) |
| `/json/{count}` | GET | Processes the first `count` dataset items, computes `total = price * quantity * m`, serializes JSON |
| `/upload` | POST | Returns the received byte count |
| `/async-db` | GET | Postgres range query (`price BETWEEN min AND max`, `LIMIT`) over the `items` table |
| `/static/{file}` | GET | Serves a file from the in-memory static cache |

## Notes

- Listens on four ports: `8080` (HTTP/1.1 + h2c), `8082` (HTTP/2-only, prior-knowledge), `8081` (HTTP/1.1-over-TLS, ALPN restricted to `http/1.1`), `8443` (HTTP/2-over-TLS)
- Static assets are read once at startup into an in-memory cache, with a pre-gzipped variant kept only when it's smaller than the plain body; `/json` responses are serialized fresh per request and compressed on the fly (256-byte threshold) when `Accept-Encoding: gzip` is present
- `/static/{file}` rejects any name that isn't a single normal path component (no traversal)
- Missing or unreachable `DATABASE_URL` is non-fatal — `/async-db` then returns an empty result set so the cleartext profiles still run
- Postgres rows are decoded by positional column index rather than by field name
- The `mq-bridge` dependency is pinned to git tag `v0.2.21` for reproducible benchmarks (the Cargo.toml notes that tracking the `dev` branch is possible but not used here)

