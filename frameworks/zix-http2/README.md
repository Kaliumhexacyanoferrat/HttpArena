# zix-http2

Zig HTTP/2 server on the `zix.Http2` raw engine (no `std.http`). One server, two listeners sharing the same per-core `.URING` worker fleet: cleartext h2c and h2 over TLS 1.3 (ALPN `h2`, self-signed Ed25519 cert baked at build time) — no second launch, no doubled workers or caches.

## Stack

- **Language:** Zig 0.16.0
- **Engine:** zix (`zix.Http2`, `.URING` dispatch model)
- **TLS:** Baked self-signed Ed25519 certificate, TLS 1.3, ALPN `h2`
- **Build:** Multi-stage, `alpine:3.20` build and runtime, musl target with `x86_64_v3+aes+pclmul+adx`/`baseline` CPU tuning

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET, POST | Sums query parameter values (plus POST body as an integer) — served over h2c |
| `/json/{count}` | GET | Renders `count` dataset items with a per-request `total = price*quantity*m` — served over h2c |
| `/static/{file}` | GET | Serves `/data/static` with `.br`/`.gz` negotiation, chunked as HTTP/2 DATA frames — served over TLS |

## Notes

- Single route table serves both listeners; routes not exercised on a given port are simply never hit by the benchmark (h2c gets baseline/json, TLS gets baseline/static)
- Static cache is pre-warmed at startup (every `.br`/`.gz`/identity candidate resolved once, single-threaded) so the request path only ever does a lock-free lookup, never an insert under load
- `+aes+pclmul+adx` CPU features are compiled in explicitly since the `x86_64_v3` baseline omits AES-NI/PCLMUL, which would otherwise force ~40x slower software AES-GCM for TLS
- Process priority elevated via `setpriority(-19)`, best-effort
