# trillium-tuned

Tuned variant of the `trillium` entry: the same Trillium 1.x handler tree, reshaped for throughput — one `current_thread` tokio runtime per core with `SO_REUSEPORT` sharding, a single shared QUIC endpoint for h3, larger/preallocated buffers, in-memory static file preloading, and `mimalloc`.

## Stack

- **Language:** Rust 1.94
- **Engine:** trillium-http (h1 + h2 prior-knowledge), trillium-quinn (h3)
- **TLS:** trillium-rustls (h1 + h2 via ALPN), trillium-quinn (QUIC)
- **gRPC:** trillium-grpc (`benchmark.BenchmarkService` over h2c + h2/TLS)
- **JSON:** sonic-rs
- **DB:** deadpool-postgres + tokio-postgres
- **Build:** Multi-stage, `-C target-cpu=native`, `debian:bookworm-slim` runtime

## Listeners

| Port | Protocol | Notes |
|------|----------|-------|
| 8080 | HTTP/1.1 cleartext + WebSocket | `SO_REUSEPORT`-sharded across per-core worker runtimes; also bound as a Unix domain socket when `LISTEN_UDS` is set |
| 8081 | HTTP/1.1 + TLS | ALPN advertises `http/1.1` only |
| 8443 (`TLS_PORT`) | HTTP/2 (ALPN) + HTTP/3 (QUIC) | h2 via TLS ALPN; h3 via a single shared QUIC endpoint on the same port/UDP |

gRPC shares the cleartext and TLS listeners, mounted first in the handler tuple ahead of compression and routing.

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline11` | GET / POST | Sums query parameter values; POST adds the body |
| `/baseline2` | GET | Same shape as `/baseline11` GET, exercised over h2/h3 |
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/json/:count` | GET | Loads `:count` items from `/data/dataset.json`, computes `total = price * quantity * m` |
| `/upload` | POST | Streams the request body and returns the byte count |
| `/static/*` | GET | Served from an in-memory preload (see Notes) |
| `/async-db` | GET | Postgres range query via `deadpool-postgres` |
| `/crud/items` | GET / POST | Paginated list / upsert |
| `/crud/items/:id` | GET / PUT | Cached read (200 ms TTL) / update with cache invalidation |
| `/ws` | GET (upgrade) | WebSocket echo |

## Notes

- One `current_thread` tokio runtime per CPU core (`WORKERS`, default = core count), each with its own `SO_REUSEPORT`-bound listener on 8080 — no cross-core work-stealing
- HTTP/3 uses a single shared QUIC endpoint rather than one per worker
- Tuned `HttpConfig`: 8 KiB response buffer, 32 MiB max/preallocated request body, 64 KiB initial body buffer, 64 KiB h2 max frame size, 64 copy-loops-per-yield
- Static files are fully preloaded into memory at startup, including precompressed `.br`/`.gz` sidecars served per `Accept-Encoding` — unlike the base `trillium` entry, which reads from disk per request
- `mimalloc` as global allocator
- CRUD cache is in-process (`DashMap`, 200 ms TTL), same mechanism as the base entry
- Optional Unix domain socket listener (`LISTEN_UDS`) alongside the TCP listeners, for the gateway profile

