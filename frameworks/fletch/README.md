# fletch

Fletch is an Express-inspired HTTP framework for Dart with radix-tree routing, middleware chains, and built-in DI. This entry runs it AOT-compiled, multi-process, on top of `dart:io`.

## Stack

- **Language:** Dart (SDK `>=3.10.0`)
- **Framework:** Fletch 2.3
- **Engine:** `dart:io`
- **Build:** `dart build cli` (AOT), `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums query parameter values (+ request body on POST) |
| `/json/:count` | GET | Returns first `count` dataset items with `total`, gzip-encoded if requested; hot combinations of count/multiplier are precomputed and cached |
| `/compression` | GET | Gzip-compressed large JSON dataset |
| `/upload` | POST | Streams the request body, returns byte count |
| `/static/:filename` | GET | Serves preloaded static files, with br/gzip precompressed variants |
| `/db` | GET | SQLite range query via a prepared statement, JSON response |
| `/async-db` | GET | Postgres range query via a connection pool, JSON response |

## Notes

- One OS process per core, each running a single isolate; cross-process `SO_REUSEPORT` sharing is enabled via an `LD_PRELOAD` shim (`reuseport_shim.c`), same approach as dart-io
- Postgres pool is opened lazily after the server starts listening, sized to `DATABASE_MAX_CONN / numberOfProcessors` per process, and torn down/rebuilt on query failure
- SQLite is opened read-only with a prepared statement reused across requests
- A second, HTTP/1.1-only TLS listener is started on port 8081 when `/certs/server.crt`/`server.key` are present (for the `json-tls` test)
- Fletch's cookie-parsing middleware is disabled (`useCookieParser: false`) since this benchmark doesn't exercise sessions
