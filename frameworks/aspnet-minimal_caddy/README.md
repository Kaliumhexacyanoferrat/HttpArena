# aspnet-minimal_caddy

ASP.NET Core minimal API running behind a Caddy reverse proxy that terminates TLS and serves HTTP/3 (QUIC) at the edge, forwarding to Kestrel over plain HTTP/1.1. Exists to measure the `gateway-h3` profile: an h3 edge in front of an h1-only backend, the shape most production ASP.NET deployments actually run.

## Stack

- **Language:** C# / .NET 10
- **Framework:** ASP.NET Core Minimal APIs
- **Engine:** Kestrel (HTTP/1.1 only, plaintext, behind the proxy)
- **Proxy:** Caddy 2 (`caddy:2-alpine`), no custom build or third-party modules
- **Build:** App image reuses `Handlers.cs`/`AppData.cs`/`Models.cs` from `aspnet-minimal`; only `Program.cs` and the Caddy config are specific to this entry

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/{count}` | GET | Returns `count` items from the preloaded dataset |
| `/async-db` | GET | Postgres range query: `SELECT ... WHERE price BETWEEN $min AND $max LIMIT $limit` |
| `/static/*` | GET | Served directly by Caddy from `/data`, not forwarded to Kestrel |

## Notes

- Caddy is the TLS/HTTP/3 endpoint: `https://localhost:8443` with `protocols h1 h2 h3` and `auto_https off` (certs supplied explicitly from `/certs`). Caddy binds 8443 on both TCP (h1/h2) and UDP (h3) from the same Caddyfile
- `/static/*` is handled entirely by Caddy's `file_server`, with `precompressed br gzip` so it prefers `.br`/`.gz` sidecars over compressing on the fly; the aspnet backend never sees these requests
- Everything else is `reverse_proxy`'d to `127.0.0.1:8080` over HTTP/1.1, with a tuned upstream transport (`keepalive 5m`, 2048 idle connections both globally and per-host) sized for a 1024-connection h2load-h3 client load
- Kestrel itself only listens on 8080 plaintext HTTP/1.1 — it has no TLS or HTTP/2+ configuration at all, since Caddy owns protocol negotiation
- Proxy and app run as separate containers (`compose.gateway-h3.yml`) with fixed CPU pinning (`cpuset`) so proxy and backend don't contend for cores
- Only the `gateway-h3` profile is wired up; the app exposes a minimal subset of the usual aspnet-minimal routes (`baseline2`, `json`, `async-db`)

