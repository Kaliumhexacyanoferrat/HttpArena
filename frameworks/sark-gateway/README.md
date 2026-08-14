# sark-gateway

sark behind a stock Caddy edge: Caddy terminates TLS and serves HTTP/1, HTTP/2 and HTTP/3, proxying dynamic requests to the sark io_uring server over plaintext HTTP/1.1 and serving `/static/*` directly from disk. Models a reverse-proxy/gateway deployment instead of exercising sark's own TLS/h2/h3 listeners.

## Stack

- **Language:** Rust 1.95 (edition 2024) — sark server
- **Runtime:** dope (io_uring, thread-per-core) — sark server
- **Edge:** Caddy 2 (`caddy:2-alpine`), TLS termination, h1/h2/h3
- **Build:** two images — Caddy edge from `proxy/Dockerfile`; sark server from the shared `../sark/Dockerfile` (`--build-arg BIN=httparena-sark`)

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/public/baseline` | GET | Sums `a`/`b` query parameters, no auth, proxied to sark |
| `/public/json/{count}` | GET | Processed dataset JSON, no auth, proxied to sark |
| `/static/*` | GET | Served directly by Caddy from disk, not proxied |

## Notes

- Caddy listens on `8443`, terminates TLS, and advertises `h1 h2 h3`; everything except `/static/*` is reverse-proxied to the sark backend at `127.0.0.1:8080` over HTTP/1.1 only
- sark runs with `SARK_HTTPARENA_H1_ONLY=1` and `SARK_HTTPARENA_PER_IP_CAP=0` — behind the proxy it serves plaintext HTTP/1.1 only, none of its own TLS/h2/h3 listeners
- sark busy-polls its io_uring driver, so its cpuset must stay disjoint from Caddy's or throughput collapses to zero
- Static files are served by Caddy with precompressed `br`/`gzip` sidecars (`file_server { precompressed br gzip }`)
- `compose.gateway.yml` and `compose.gateway-h3.yml` are identical topologies — the `gateway-64` and `gateway-h3` test profiles reuse the same Caddy + sark stack and differ only in the protocol the client negotiates with Caddy

