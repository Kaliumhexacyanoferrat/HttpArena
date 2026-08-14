# ring

Standard Ring application (official `ring/ring-core` + `ring/ring-jetty-adapter`) with `wrap-params` middleware, wired manually for virtual threads.

## Stack

- **Language:** Clojure 1.12 (deps.edn / `clojure` CLI)
- **Framework:** Ring 1.15.5 (`ring.middleware.params`)
- **Server:** Jetty via official `ring/ring-jetty-adapter`, virtual-thread `QueuedThreadPool`
- **Build:** `clojure:tools-deps-trixie-slim`, `clojure -X:deps prep`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/async-db` | GET | Blocking JDBC range query via HikariCP |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/static/:filename` | GET | Serves files from `/data/static` |

## Notes

- Uses the official `ring/ring-jetty-adapter`, distinct from the community `ring-jetty9-adapter` entry
- Virtual threads wired manually: a `QueuedThreadPool` with `VirtualThreads/getDefaultVirtualThreadsExecutor` is passed as `:thread-pool`
- Jetty `GzipHandler` installed via the adapter's `:configurator`, excluding `/static/*`
- HikariCP datasource lazily created on first `/async-db` request (double-checked locking)
