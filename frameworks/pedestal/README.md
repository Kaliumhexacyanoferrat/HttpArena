# pedestal

Pedestal service built with the newer connector-map API (`io.pedestal.connector`), backed by Jetty and a virtual-thread pool.

## Stack

- **Language:** Clojure 1.12 (deps.edn / `clojure` CLI)
- **Framework:** Pedestal 0.8.2-beta-10 (`io.pedestal.connector`, connector-map API)
- **Server:** Jetty (`io.pedestal.http.jetty`), virtual-thread `QueuedThreadPool`
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

- Uses Pedestal's newer connector-map API (`io.pedestal.connector`) rather than the classic `io.pedestal.http` service map
- Explicit `not-found` and `query-params` interceptors added — Pedestal doesn't parse query params by default
- Jetty thread pool backed by virtual threads (`VirtualThreads/getDefaultVirtualThreadsExecutor`)
- Jetty `GzipHandler` inserted into the servlet context, excluding `/static/*`
