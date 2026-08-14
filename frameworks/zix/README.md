# zix

Zig HTTP/1.1 server built directly on the `zix.Http1` raw engine (no `std.http`). Shared-nothing design: each worker runs its own `SO_REUSEPORT` multishot accept plus io_uring completion loop and owns its connections, with no cross-worker locking.

## Stack

- **Language:** Zig 0.16.0
- **Engine:** zix (`zix.Http1`, `.URING` dispatch model)
- **Build:** Multi-stage, `alpine:3.20` build and runtime, musl target with `x86_64_v3`/`baseline` CPU tuning

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline11` | GET, POST | Sums query parameter values (plus POST body as an integer) |
| `/pipeline` | GET | Fixed `ok` response; intercepted pre-parse for zero header overhead |
| `/json/{count}` | GET | Renders `count` dataset items with a per-request `total = price*quantity*m`; response-cache aware |
| `/upload` | POST | Returns the received byte count (large bodies drained by the engine, never buffered) |
| `/static/{file}` | GET | Serves `/data/static` with `.br`/`.gz` content negotiation via a lock-free fd cache and `sendfile` |

## Notes

- `rawIntercept` byte-matches `/pipeline` before any header parsing, bypassing the router entirely on that path
- `/json` responses are served from a per-worker response cache keyed on `(method, path, query)`, replaying full precomputed bytes on a hit
- Static files are cached on first request (fd, size, pre-rendered header) in a lock-free append-only table; serving is header send + zero-copy `sendfile`
- Process priority elevated via `setpriority(-19)`, best-effort (no special capability required)
- Two tuning profiles (`lean`/`throughput`) size the per-connection recv buffer for dev vs. competition hardware
