# ring-jetty9-adapter

Standard Ring application (`wrap-params` middleware) served via Sunng's [`ring-jetty9-adapter`](https://github.com/sunng87/ring-jetty9-adapter), an actively maintained alternative to the official Ring Jetty adapter with built-in virtual-thread support.

## Stack

- **Language:** Clojure 1.12 (deps.edn / `clojure` CLI)
- **Framework:** Ring (`ring.middleware.params`)
- **Server:** Jetty via `info.sunng/ring-jetty9-adapter` 0.40.3, `:virtual-threads? true`
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

- Uses the community `ring-jetty9-adapter` (Sunng), distinct from the official `ring/ring-jetty-adapter` used by the `ring` entry
- Virtual threads enabled via the adapter's own `:virtual-threads?` option
- Jetty `GzipHandler` wraps the whole Ring handler, excluding `/static/*`
- HikariCP datasource lazily created on first `/async-db` request (double-checked locking)
